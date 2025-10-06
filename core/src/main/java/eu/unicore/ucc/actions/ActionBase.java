package eu.unicore.ucc.actions;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.Option;

import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.Command;
import eu.unicore.ucc.UCCOptions;
import eu.unicore.ucc.actions.data.Resolve;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.authn.UCCConfigurationProviderImpl;
import eu.unicore.ucc.authn.UsernameAuthN;
import eu.unicore.ucc.io.Location;
import eu.unicore.ucc.util.MultiRegistryClient;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Base class for (RESTful) UCC actions.
 * 
 * Sets up registry and security.
 * 
 * @author schuller
 */
public abstract class ActionBase extends Command {

	protected IRegistryClient registry;

	protected UCCConfigurationProvider configurationProvider;
	
	protected String registryURL;

	protected String authNMethod;

	protected boolean acceptAllIssuers = false;
	
	protected String[] blacklist;
	
	/**
	 * creates basic options for setting keystore, etc
	 */
	@Override
	protected void createOptions() {
		super.createOptions();
		createGeneralOptions();
		createSecurityOptions();
	}

	/*
	 * security related (keystore, truststore, ...)
	 */
	private void createSecurityOptions(){

		getOptions().addOption(Option.builder(OPT_AUTHN_METHOD)
				.longOpt(OPT_AUTHN_METHOD_LONG)
				.desc("The method used for authentication")
				.argName("Method")
				.hasArg()
				.required(false)
				.get()
				,UCCOptions.GRP_SECURITY);

		getOptions().addOption(Option.builder(OPT_AUTHN_ACCEPT_ALL)
				.longOpt(OPT_AUTHN_ACCEPT_ALL_LONG)
				.desc("Accept issuers not in the trust store")
				.required(false)
				.get()
				,UCCOptions.GRP_SECURITY);
		
		getOptions().addOption(Option.builder(OPT_SECURITY_PREFERENCES)
				.longOpt(OPT_SECURITY_PREFERENCES_LONG)
				.desc("User preference regarding choice of UNIX login and " +
						"groups, role etc which should be used for operation execution. " +
						"The selected values must be allowed for the user. " +
						"Full syntax: "  + UCCConfigurationProviderImpl.PREFERENCE_ARG_HELP)
						.argName(UCCConfigurationProviderImpl.PREFERENCE_ARG)
						.hasArgs()
						.required(false)
						.get()
						,UCCOptions.GRP_SECURITY);
	}


	/* 
	 * general (preferences, registries, ...)
	 */
	private void createGeneralOptions(){
		getOptions().addOption(Option.builder(OPT_PROPERTIES)
				.longOpt(OPT_PROPERTIES_LONG)
				.desc("Properties file containing your preferences. By default, a file '<userhome>/.ucc/preferences' is checked.")
				.argName("File")
				.hasArg()
				.required(false)
				.get()
				,UCCOptions.GRP_GENERAL);

		getOptions().addOption(Option.builder(OPT_REGISTRY)
				.longOpt(OPT_REGISTRY_LONG)
				.desc("Comma-separated list of UNICORE registry URLs")
				.argName("Registry")
				.hasArg()
				.required(false)
				.get()
				,UCCOptions.GRP_GENERAL);
	}

	@Override
	public void process() throws Exception {
		super.process();
		initConfigurationProvider();
		initRegistryClient();
		setOutputLocation();
		String blacklistP = properties.getProperty("blacklist", null);
		if(blacklistP!=null) {
			blacklist = blacklistP.split("[ ,]+");
		}else blacklist = new String[0];
		if(blacklist.length>0) {
			console.debug("Blacklist = {}", Arrays.asList(blacklist));
		}
	}

	@Override
	public void postProcess(){
		super.postProcess();
		try{
			configurationProvider.flushSessions();
		}catch(Exception ex){
			console.error(ex, "Could not store session IDs");
		}
	}

	public UCCConfigurationProvider getConfigurationProvider() {
		return configurationProvider;
	}

	/**
	 * Function that returns true if the connection to registry is not required.
	 * By default, this checks a property "contact-registry". If this is not
	 * set, returns <code>true</code>, otherwise the value is parsed as a boolean. 
	 * Override if needed.
	 */
	protected boolean skipConnectingToRegistry(){
		return !Boolean.parseBoolean(properties.getProperty("contact-registry", "true"));
	}

	/**
	 * return <code>true</code> if the command REQUIRES an option or configuration value for the
	 * registry 
	 */
	protected boolean requireRegistry(){
		return true;
	}

	/**
	 * returns true if registry access should use the credentials.
	 * if false, anonymous access will be used to read the registry
	 */
	protected boolean authenticateToRegistry(){
		return Boolean.parseBoolean(properties.getProperty("authenticate-to-registry", "false"));
	}

	/**
	 * initialise the configuration provider
	 * 
	 * @throws IOException
	 */
	public void initConfigurationProvider()throws Exception{
		authNMethod = getOption(OPT_AUTHN_METHOD_LONG, OPT_AUTHN_METHOD, UsernameAuthN.NAME);
		acceptAllIssuers = getBooleanOption(OPT_AUTHN_ACCEPT_ALL_LONG, OPT_AUTHN_ACCEPT_ALL);
		if(acceptAllIssuers){
			console.debug("Accepting all server CA certificates.");
		}
		configurationProvider = new UCCConfigurationProviderImpl(authNMethod, properties, this, acceptAllIssuers);
	}

	/**
	 * setup the registry client to be used. The registry URL is taken from the 
	 * commandline or the properties file. If no registry is configured, registry connection
	 * will not be done. This may or not lead to issues later, depending on the type of command
	 */
	protected void initRegistryClient() throws Exception {
		registryURL = getCommandLine().getOptionValue(OPT_REGISTRY, properties.getProperty(OPT_REGISTRY_LONG));
		if(registryURL==null || registryURL.trim().length()==0){
			console.debug("No registry is configured.");
			if(requireRegistry()){
				throw new Exception("A registry is required: please use the '-r' option " +
						"or configuration entry to define the registry to be used.");
			}
			return;
		}
		if(skipConnectingToRegistry() && !requireRegistry()){
			console.debug("Registry connection will be skipped.");
			return;
		}
		//accept list of registries either comma- or space-separated
		String[] urls = registryURL.split("[, ]");
		if(urls.length>1){
			MultiRegistryClient erc = new MultiRegistryClient();
			for(String url: urls){
				if(url.trim().length()==0)continue;
				console.debug("Registry = {}", url);
				erc.addRegistry(makeRegistry(url));
			}
			registry = erc;
		}
		else{
			console.debug("Registry = {}", registryURL);
			registry = makeRegistry(registryURL);
		}
		testRegistryConnection();
	}

	protected IRegistryClient makeRegistry(String url)throws Exception{
		IClientConfiguration sec = configurationProvider.getClientConfiguration(url);
		IAuthCallback auth = authenticateToRegistry()? configurationProvider.getRESTAuthN() :
			configurationProvider.getAnonymousRESTAuthN();
		return new RegistryClient(url, sec, auth);
	}

	protected void testRegistryConnection() throws Exception {
		if (registry == null)
		{
			throw new Exception("Registry access is not initialized");
		}
		try{
			console.debug("Checking registry connection.");
			String status = registry.getConnectionStatus();
			console.debug("Registry connection status: {}", status);
			if(!status.startsWith("OK") && !skipConnectingToRegistry()) {
				throw new Exception(status);
			}
		}catch(Exception e){
			throw new Exception("Cannot contact registry (set 'contact-registry=false' to ignore this error) ",e);
		}
	}

	protected Location createLocation(String descriptor) {
		return Resolve.resolve(descriptor, registry, configurationProvider);
	}

	protected boolean isBlacklisted(String url) {
		if(blacklist.length>0) {
			for(String b: blacklist) {
				if(url.contains(b))return true;
			}
		}
		return false;
	}
}