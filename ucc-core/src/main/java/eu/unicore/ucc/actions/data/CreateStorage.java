package eu.unicore.ucc.actions.data;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.Option;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.ucc.IServiceInfoProvider;
import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.util.PropertyVariablesResolver;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.lookup.StorageFactoryLister;
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
	private String storageType;

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
				.build());
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Name of the site")
				.argName("Site")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_FACTORY)
				.longOpt(OPT_FACTORY_LONG)
				.desc("Factory URL")
				.argName("Factory")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_TYPE)
				.longOpt(OPT_TYPE_LONG)
				.desc("Storage type")
				.argName("Type")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_INFO)
				.longOpt(OPT_INFO_LONG)
				.desc("Only show info, do not create anything")
				.required(false)
				.build());
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
	public void process() {
		super.process();

		initialLifeTime=getNumericOption(OPT_LIFETIME_LONG, OPT_LIFETIME, -1);
		if(initialLifeTime>0){
			verbose("New SMSs will have a lifetime of <"+initialLifeTime+"> days.");
		}else{
			verbose("Using site default for SMS lifetime.");
		}
		
		factoryURL=getOption(OPT_FACTORY_LONG, OPT_FACTORY);
		StorageFactoryClient sfc=null;

		if(factoryURL==null){
			siteName=getOption(OPT_SITENAME_LONG, OPT_SITENAME);
			if(siteName!=null){
				verbose("Looking for factory at site <"+siteName+">");
			}
			else{
				verbose("No factory specified, will choose one.");
			}
		}

		storageType=getOption(OPT_TYPE_LONG, OPT_TYPE);
		if(storageType!=null){
			verbose("Will create storage of type <"+storageType+">");
		}
		else{
			verbose("No storage type specified, will use factory's default.");
		}
		
		boolean infoOnly = getBooleanOption(OPT_INFO_LONG, OPT_INFO);

		//resolve
		boolean byFactoryURL = factoryURL!=null;
		boolean byType = storageType!=null ;
		boolean bySiteName = siteName!=null ;
		Filter filter = new Filter(byFactoryURL,bySiteName,byType);
		StorageFactoryLister sfl = new StorageFactoryLister(
				UCC.executor, registry, configurationProvider, filter);
		sfl.setTimeout(2, TimeUnit.SECONDS);

		Iterator<StorageFactoryClient>iter = sfl.iterator();
		boolean haveValidFactory = false;
		while(iter.hasNext()){
			sfc=sfl.iterator().next();
			if(sfc==null){
				error("No suitable storage factory available!",null);
				endProcessing(ERROR);
			}
			factoryURL = sfc.getEndpoint().getUrl();
			verbose("Using factory at <"+factoryURL+">");
			haveValidFactory=true;
			try{
				if(infoOnly){
					message(getDescription(sfc, true));
				}
				else{
					doCreate(sfc);
				}
				break;
			}catch(Exception ex){
				String err = infoOnly? "Error" : "Could not create storage"; 
				error(err,ex);
				endProcessing(ERROR);
			}
		}
		if(!haveValidFactory){
			// nothing found
			error("No suitable storage factory available!",null);
			endProcessing(ERROR);
		}
	}

	protected void doCreate(StorageFactoryClient sfc) throws Exception{
		Calendar tt = null;
		if(initialLifeTime>0){
			tt = Calendar.getInstance();
			tt.add(Calendar.DATE, initialLifeTime);
		}
		StorageClient sc = sfc.createStorage(storageType, null, getParams(), tt);
		String addr=sc.getEndpoint().getUrl();
		message(addr);
		properties.put(PROP_LAST_RESOURCE_URL, addr);
		setLastStorageAddress(addr);
	}

	protected Map<String,String> getParams() throws IOException {
		String[]args = getCommandLine().getArgs();
		if(args.length>1){
			// other parameters from the cmdline
			String[] paramArgs = new String[args.length-1];
			System.arraycopy(args, 1, paramArgs, 0, paramArgs.length);
			return PropertyVariablesResolver.getParameters(paramArgs);
		}
		else return null;
	}

	protected Calendar getTermTime(){
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, initialLifeTime);
		return c;
	}

	@Override
	public String getCommandGroup(){
		return "General";
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
			StorageFactoryClient smf=new StorageFactoryClient(epr, securityProperties, configurationProvider.getRESTAuthN());
			return getDescription(smf, true);
		}catch(Exception ex){
			return "N/A ["+Log.getDetailMessage(ex)+"]";
		}
	}

	protected String getDescription(StorageFactoryClient sfc, boolean addParamInfo) throws Exception {
		JSONObject pr=sfc.getProperties().getJSONObject("storageDescriptions");
		StringBuilder sb=new StringBuilder();
		boolean first=true;
		String newline = System.getProperty("line.separator");
		Iterator<String>types = pr.keys();
		while(types.hasNext()){
			if(!first)sb.append(newline).append("  ");
			String type = types.next();
			JSONObject desc = pr.getJSONObject(type);
			sb.append(getBriefDescription(type, desc));
			if(addParamInfo){
				sb.append(newline).append(getParameterDescription(desc));
			}
			first=false;
		}
		return sb.toString();
	}

	protected String getBriefDescription(String type, JSONObject desc){
		StringBuilder sb=new StringBuilder();
		sb.append(type);
		try{
			// TODO
		}catch(Exception ex){}
		return sb.toString();
	}

	protected String getParameterDescription(JSONObject desc) throws JSONException {
		return desc.toString(2);
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
				JSONObject desc = smf.getProperties().getJSONObject("storageDescriptions");
				Iterator<String>types = desc.keys();
				while(types.hasNext()){
					if(storageType.equals(types.next()))return true;
				}
			}catch(Exception ex){
				error("Error checking factory at <"+smf.getEndpoint().getUrl()+">",ex);
			}
			return false;
		}

	}

}
