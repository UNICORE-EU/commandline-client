package eu.unicore.ucc.actions;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.OptionBuilder;

import de.fzj.unicore.ucc.Command;
import de.fzj.unicore.ucc.UCCOptions;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.authn.UCCConfigurationProviderImpl;
import de.fzj.unicore.ucc.authn.UsernameAuthN;
import de.fzj.unicore.ucc.helpers.EndProcessingException;
import de.fzj.unicore.ucc.util.MultiRegistryClient;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.ucc.actions.data.Resolve;
import eu.unicore.ucc.io.Location;
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
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		createGeneralOptions();
		createSecurityOptions();
	}

	/*
	 * security related (keystore, truststore, ...)
	 */
	@SuppressWarnings({ "static-access"})
	private void createSecurityOptions(){

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_AUTHN_METHOD_LONG)
				.withDescription("The method used for authentication")
				.withArgName("AuthenticationMethod")
				.hasArg()
				.isRequired(false)
				.create(OPT_AUTHN_METHOD)
				,UCCOptions.GRP_SECURITY);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_AUTHN_ACCEPT_ALL_LONG)
				.withDescription("Accept issuers not in the trust store")
				.isRequired(false)
				.create(OPT_AUTHN_ACCEPT_ALL)
				,UCCOptions.GRP_SECURITY);
		
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SECURITY_PREFERENCES_LONG)
				.withDescription("User preference regarding choice of UNIX login and " +
						"groups, role etc which should be used for operation execution. " +
						"The selected values must be allowed for the user. " +
						"Full syntax: "  + UCCConfigurationProviderImpl.PREFERENCE_ARG_HELP)
						.withArgName(UCCConfigurationProviderImpl.PREFERENCE_ARG)
						.hasArgs()
						.isRequired(false)
						.create(OPT_SECURITY_PREFERENCES)
						,UCCOptions.GRP_SECURITY);
	}


	/* 
	 * general (preferences, registries, ...)
	 */
	@SuppressWarnings("static-access")
	private void createGeneralOptions(){
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_PROPERTIES_LONG)
				.withDescription("Properties file containing your preferences. By default, a file '<userhome>/.ucc/preferences' is checked.")
				.withArgName("Properties")
				.hasArg()
				.isRequired(false)
				.create(OPT_PROPERTIES)
				,UCCOptions.GRP_GENERAL);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_REGISTRY_LONG)
				.withDescription("Comma-separated list of UNICORE registry URLs")
				.withArgName("Registry")
				.hasArg()
				.isRequired(false)
				.create(OPT_REGISTRY)
				,UCCOptions.GRP_GENERAL);
	}

	@Override
	public void process(){
		super.process();
		try{
			initConfigurationProvider();
		}catch(Exception ioe){
			error("Problem setting up security.",ioe);
			endProcessing(ERROR_SECURITY);
		}
		
		initRegistryClient();
		
		setOutputLocation();
		
		String blacklistP = properties.getProperty("blacklist", null);
		if(blacklistP!=null) {
			blacklist = blacklistP.split("[ ,]+");
		}else blacklist = new String[0];
		if(verbose && blacklist.length>0) {
			verbose("Blacklist = "+Arrays.asList(blacklist));
		}
	}
	
	@Override
	public void postProcess(){
		super.postProcess();
		try{
			configurationProvider.flushSessions();
		}catch(Exception ex){
			error("Could not store session IDs", ex);
		}
	}

	public UCCConfigurationProvider getConfigurationProvider() {
		return configurationProvider;
	}
	
	public void setUCCClientConfigurationProvider(UCCConfigurationProvider configurationProvider) {
		this.configurationProvider = configurationProvider;
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
	 * initialise the configuration provider
	 * 
	 * @throws IOException
	 */
	public void initConfigurationProvider()throws Exception{
		if (configurationProvider != null) //unit testing
			return;
		String authNMethod = getOption(OPT_AUTHN_METHOD_LONG, OPT_AUTHN_METHOD, UsernameAuthN.NAME);
		acceptAllIssuers = getBooleanOption(OPT_AUTHN_ACCEPT_ALL_LONG, OPT_AUTHN_ACCEPT_ALL);
		if(acceptAllIssuers){
			verbose("Accepting all server CA certificates.");
		}
		configurationProvider = new UCCConfigurationProviderImpl(authNMethod, properties, this, acceptAllIssuers);
		this.authNMethod = authNMethod;
	}

	/**
	 * setup the registry client to be used. The registry URL is taken from the 
	 * commandline or the properties file. If no registry is configured, registry connection
	 * will not be done. This may or not lead to issues later, depending on the type of command
	 */
	protected void initRegistryClient(){
		registryURL=getCommandLine().getOptionValue(OPT_REGISTRY, properties.getProperty(OPT_REGISTRY_LONG));
		if(registryURL==null || registryURL.trim().length()==0){
			verbose("No registry is configured.");
			if(requireRegistry()){
				throw new EndProcessingException(ERROR, "A registry is required: please use the '-r' option " +
						"or configuration entry to define the registry to be used.");
			}
			return;
		}
		if(skipConnectingToRegistry() && !requireRegistry()){
			verbose("Registry connection will be skipped.");
			return;
		}
		verbose("Registry = "+registryURL);
		//accept list of registries either comma- or space-separated
		String[] urls=registryURL.split("[, ]");
		if(urls.length>1){
			MultiRegistryClient erc= new MultiRegistryClient(this);
			for(String url: urls){
				if(url.trim().length()==0)continue;
				verbose("Registry = "+url);
				try{
					IRegistryClient c = makeRegistry(url);
					erc.addRegistry(c);
				}catch(Exception e){
					error("Cannot contact registry <"+url+">",null);
				}	
			}
			registry=erc;
		}
		else{
			try{
				registry = makeRegistry(registryURL);
				testRegistryConnection();
			}catch(Exception e){
				error("Cannot contact registry",e);
				endProcessing(ERROR_CONNECTION);
			}
		}
	}

	protected IRegistryClient makeRegistry(String url)throws Exception{
		if(url.contains("/services/Registry")){
			url = convertToREST(url);
			verbose("Using converted Registry URL "+url);
		}
		IClientConfiguration sec = configurationProvider.getClientConfiguration(url);
		IAuthCallback auth = configurationProvider.getRESTAuthN();
		return new RegistryClient(url, sec, auth);
	}

	private String convertToREST(String soapURL) {
		String base = soapURL.split("/services/")[0];
		String regID = soapURL.split("res=")[1]; 
		return base+"/rest/registries/"+regID;
	}
	/**
	 * checks if the registry can be contacted
	 * 
	 */
	protected void testRegistryConnection(){
		if (registry == null)
		{
			message("Registry access is not initialized");
			endProcessing(ERROR_CLIENT);
			return;
		}
		try{
			verbose("Checking registry connection.");
			String status = registry.getConnectionStatus();
			verbose("Registry connection status: "+status);
			if(!status.startsWith("OK") && !skipConnectingToRegistry()) {
				throw new Exception(status);
			}
		}catch(Exception e){
			error("Cannot contact registry (set 'contact-registry=false' to ignore this error) ",e);
			endProcessing(ERROR_CLIENT);
		}
	}
	
	protected Location createLocation(String descriptor) {
		return Resolve.resolve(descriptor, registry, configurationProvider, this);
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