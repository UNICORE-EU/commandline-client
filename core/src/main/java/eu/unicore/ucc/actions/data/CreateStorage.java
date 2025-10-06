package eu.unicore.ucc.actions.data;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.Option;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.IServiceInfoProvider;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.lookup.StorageFactoryLister;
import eu.unicore.ucc.util.PropertyVariablesResolver;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * creates an SMS instance using the StorageFactory service
 * 
 * @author schuller
 */
public class CreateStorage extends ActionBase implements IServiceInfoProvider {

	/**
	 * the initial lifetime (in days) for newly created SMSs
	 */
	private int initialLifeTime;

	/**
	 * factory URL to use
	 */
	private String factoryURL;

	/**
	 * site where to create the storage
	 */
	private String siteName;

	/**
	 * storage type to create
	 */
	private String storageType = "DEFAULT";

	public static final String OPT_TYPE_LONG="type";
	public static final String OPT_TYPE="t";

	public static final String OPT_INFO_LONG="info";
	public static final String OPT_INFO="i";

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_LIFETIME)
				.longOpt(OPT_LIFETIME_LONG)
				.desc("Initial lifetime (in days) for created storages.")
				.argName("Lifetime")
				.hasArg()
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Name of the site")
				.argName("Site")
				.hasArg()
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_FACTORY)
				.longOpt(OPT_FACTORY_LONG)
				.desc("Factory URL to use")
				.argName("Factory")
				.hasArg()
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_TYPE)
				.longOpt(OPT_TYPE_LONG)
				.desc("Storage type")
				.argName("Type")
				.hasArg()
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_INFO)
				.longOpt(OPT_INFO_LONG)
				.desc("Only show info, do not create anything")
				.required(false)
				.get());
	}

	@Override
	public String getName(){
		return "create-storage";
	}

	@Override
	public String getArgumentList(){
		return "[param1=value1,...]";
	}

	@Override
	public String getSynopsis(){
		return "Creates a storage service instance. " +
				"The StorageFactory to be used can be specified. Optionally, " +
				"the type of storage plus any additional parameters can be given.";

	}
	@Override
	public String getDescription(){
		return "create a storage service instance";
	}

	@Override
	public void process() throws Exception {
		lastStorageAddress = null;
		super.process();
		initialLifeTime = getNumericOption(OPT_LIFETIME_LONG, OPT_LIFETIME, -1);
		factoryURL = getOption(OPT_FACTORY_LONG, OPT_FACTORY);
		StorageFactoryClient sfc = null;
		if(factoryURL==null){
			siteName = getOption(OPT_SITENAME_LONG, OPT_SITENAME);
			storageType = getOption(OPT_TYPE_LONG, OPT_TYPE);
		}
		boolean infoOnly = getBooleanOption(OPT_INFO_LONG, OPT_INFO);
		// resolve
		boolean byFactoryURL = factoryURL!=null;
		boolean byType = storageType!=null ;
		boolean bySiteName = siteName!=null ;
		Filter filter = new Filter(byFactoryURL,bySiteName,byType);
		StorageFactoryLister sfl = new StorageFactoryLister(
				UCC.executor, registry, configurationProvider, filter);
		sfl.setTimeout(2);
		Iterator<StorageFactoryClient>iter = sfl.iterator();
		boolean haveValidFactory = false;
		while(iter.hasNext()){
			sfc = iter.next();
			if(sfc==null){
				throw new Exception("No suitable storage factory available!");
			}
			factoryURL = sfc.getEndpoint().getUrl();
			console.verbose("Using factory at <{}>", factoryURL);
			haveValidFactory=true;
			if(infoOnly){
				console.info("{}", getDescription(sfc));
			}
			else{
				doCreate(sfc);
			}
			break;
		}
		if(!haveValidFactory){
			throw new Exception("No suitable storage factory available!");
		}
	}

	@SuppressWarnings("deprecation")
	private void doCreate(StorageFactoryClient sfc) throws Exception{
		boolean u10Mode = sfc.getEndpoint().getUrl().contains("default_storage_factory");
		StorageClient sc = u10Mode ?
				sfc.createStorage(storageType, null, getParams(), getTermTime()) :
				sfc.createStorage(siteName, getParams(), getTermTime());
		String addr = sc.getEndpoint().getUrl();
		console.info("{}", addr);
		properties.put(PROP_LAST_RESOURCE_URL, addr);
		setLastStorageAddress(addr);
	}

	private Map<String,String> getParams() throws IOException {
		String[]args = getCommandLine().getArgs();
		if(args.length>1){
			// other parameters from the cmdline
			String[] paramArgs = new String[args.length-1];
			System.arraycopy(args, 1, paramArgs, 0, paramArgs.length);
			return PropertyVariablesResolver.getParameters(paramArgs);
		}
		else return null;
	}

	private Calendar getTermTime(){
		Calendar tt = null;
		if(initialLifeTime>0){
			tt = Calendar.getInstance();
			tt.add(Calendar.DATE, initialLifeTime);
		}
		return tt;
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_DATA;
	}

	@Override
	public String getType(){
		return "StorageFactory";
	}

	@Override
	public String getServiceName(){
		return "StorageFactory";
	}

	@Override
	public String getServiceDetails(Endpoint epr, UCCConfigurationProvider configurationProvider){
		try{
			IClientConfiguration securityProperties = configurationProvider.getClientConfiguration(epr.getUrl());
			StorageFactoryClient smf = new StorageFactoryClient(epr, securityProperties, configurationProvider.getRESTAuthN());
			return getDescription(smf);
		}catch(Exception ex){
			return "N/A ["+Log.getDetailMessage(ex)+"]";
		}
	}

	private String getDescription(StorageFactoryClient sfc) throws Exception {
		boolean u11mode = true;
		JSONObject pr = sfc.getProperties().optJSONObject("storageDescriptions");
		if(pr==null) {
			u11mode = false;
			pr = new JSONObject();
			pr.put("description", sfc.getProperties().optString("description", "n/a"));
			pr.put("parameters", sfc.getProperties().optJSONObject("parameters", new JSONObject()));
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		if(u11mode) {
			sb.append(": ").append(pr.getString("description"));
			sb.append(_newline).append(getParameterDescription(pr));
		}
		else {
			Iterator<String>types = pr.keys();
			while(types.hasNext()){
				if(!first)sb.append(_newline).append("  ");
				String type = types.next();
				JSONObject desc = pr.getJSONObject(type);
				sb.append(getBriefDescription(type, desc));
				sb.append(_newline).append(getParameterDescription(desc));
				first=false;
			}
		}
		return sb.toString();
	}

	private String getBriefDescription(String type, JSONObject desc){
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		try{
			String description = desc.optString("description", null);
			if(description!=null) {
				sb.append(": ").append(description);
			}
		}catch(Exception ex){}
		return sb.toString();
	}

	private String getParameterDescription(JSONObject desc) throws JSONException {
		StringBuilder sb = new StringBuilder();
		sb.append("  Parameters:");
		JSONObject parameterDesc = desc.optJSONObject("parameters", new JSONObject());
		Iterator<String> params = parameterDesc.keys();
		while(params.hasNext()){
			sb.append(_newline).append("    ");
			String p = params.next();
			String pDesc = parameterDesc.optString(p, "n/a");
			sb.append(p).append(": ").append(pDesc);
		}	
		return sb.toString();
	}

	private static String lastStorageAddress;

	public static String getLastStorageAddress() {
		return lastStorageAddress;
	}

	protected static void setLastStorageAddress(String lastAddress) {
		CreateStorage.lastStorageAddress = lastAddress;
	}


	private class Filter implements AddressFilter {

		private boolean byFactoryURL;
		private boolean bySiteName;
		private boolean byType;

		public Filter(boolean byFactoryURL, boolean bySiteName, boolean byType){
			this.byFactoryURL = byFactoryURL;
			this.bySiteName = bySiteName;
			this.byType = byType;
		}

		@Override
		public boolean accept(Endpoint epr) {
			return accept(epr.getUrl());
		}

		@Override
		public boolean accept(String address) {
			if(byFactoryURL){
				return address.equalsIgnoreCase(factoryURL);
			}
			else if (bySiteName){
				return address.contains("/"+siteName+"/");
			}
			else return true;
		}

		@Override
		public boolean accept(BaseServiceClient smf) {
			return checkStorageType(smf); 
		}

		private boolean checkStorageType(BaseServiceClient smf) {
			try{
				if(!byType)return true;
				JSONObject desc = smf.getProperties().optJSONObject("storageDescriptions");
				if(desc==null) {
					return new File(smf.getEndpoint().getUrl()).getName().equalsIgnoreCase(storageType);
				}
				// u10 and older has the available types as properties
				Iterator<String>types = desc.keys();
				while(types.hasNext()){
					if(storageType.equals(types.next()))return true;
				}
			}catch(Exception ex){
				console.error(ex, "Error checking factory at <{}>", smf.getEndpoint().getUrl());
			}
			return false;
		}
	}

	@Override
	public Collection<Endpoint> listEndpoints(IRegistryClient registry, UCCConfigurationProvider configurationProvider) throws Exception {
		Set<Endpoint> ep = new HashSet<>();
		ep.addAll(registry.listEntries(new RegistryClient.ServiceTypeFilter(getType())));
		List<Endpoint> coreEps = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
		IAuthCallback auth = configurationProvider.getRESTAuthN();
		for(Endpoint c: coreEps) {
			IClientConfiguration sec = configurationProvider.getClientConfiguration(factoryURL);
			CoreClient cc = new CoreClient(c, sec, auth);
			String url = cc.getLinkUrl("storagefactories");
			EnumerationClient ec = new EnumerationClient(c.cloneTo(url), sec, auth);
			ec.forEach((smf)->{
				if(!contains(ep,smf))ep.add(c.cloneTo(smf));
			});
		}
		return ep;
	}

	private boolean contains(Collection<Endpoint>eps, String url) {
		final AtomicBoolean found = new AtomicBoolean();
		eps.forEach( (e)-> {
				if(url.equals(e.getUrl())) {
					found.set(true);
				}
			});
		return found.get();
	}
}
