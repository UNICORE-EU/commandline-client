package eu.unicore.ucc.actions.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.lookup.SiteNameFilter;
import eu.unicore.ucc.IServiceInfoProvider;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.lookup.SiteFactoryLister;
import eu.unicore.ucc.util.PropertyVariablesResolver;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * creates an TSS instance
 * 
 * @author schuller
 */
public class CreateTSS extends ActionBase implements IServiceInfoProvider {

	/**
	 * the initial lifetime (in days) for newly created TSSs
	 */
	private int initialLifeTime;

	/**
	 * factory URL to use
	 */
	private String factoryURL;

	/**
	 * site where to create the TSS
	 */
	private String siteName;

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
	}

	@Override
	public String getName(){
		return "create-tss";
	}

	@Override
	public String getArgumentList(){
		return "[param1=value1,...]";
	}

	@Override
	public String getSynopsis(){
		return "Creates a target system service instance. " +
				"The TargetSystemFactory to be used can be specified. Optionally, " +
				"additional parameters can be given.";

	}
	@Override
	public String getDescription(){
		return "create a target system service instance";
	}

	@Override
	public void process() {
		super.process();

		initialLifeTime = getNumericOption(OPT_LIFETIME_LONG, OPT_LIFETIME, -1);
		if(initialLifeTime>0){
			verbose("New TSSs will have a lifetime of <"+initialLifeTime+"> days.");
		}else{
			verbose("Using site default for TSS lifetime.");
		}

		factoryURL = getOption(OPT_FACTORY_LONG, OPT_FACTORY);
		SiteFactoryClient tsf=null;

		try{
			if(factoryURL==null){
				SiteFactoryLister tsfl = new SiteFactoryLister(UCC.executor, registry, configurationProvider);
				siteName = getOption(OPT_SITENAME_LONG, OPT_SITENAME);
				if(siteName!=null){
					verbose("Looking for factory at site <"+siteName+">");
					tsfl.setAddressFilter(new SiteNameFilter(siteName));
				}
				else{
					verbose("No factory specified, will choose one from registry.");
				}
				tsf = tsfl.iterator().next();
			}
			else{
				verbose("Using factory at <"+factoryURL+">");
				tsf = new SiteFactoryClient(new Endpoint(factoryURL), 
						configurationProvider.getClientConfiguration(factoryURL), 
						configurationProvider.getRESTAuthN());
			}

			if(tsf==null){
				error("No suitable target system factory available!",null);
				endProcessing(ERROR);
			}
			else{
				factoryURL = tsf.getEndpoint().getUrl();
				verbose("Using factory at <"+factoryURL+">");
			}

			SiteClient tss = tsf.createSite(getCreationParameters(), getTermTime());
			String addr = tss.getEndpoint().getUrl();
			message(addr);
			properties.put(PROP_LAST_RESOURCE_URL, addr);
			setLastTSSAddress(addr);
		}catch(Exception ex){
			error("Could not create site",ex);
			endProcessing(ERROR);
		}
	}
	
	protected Map<String,String> getCreationParameters() throws IOException {
		String[]args = getCommandLine().getArgs();
		if(args.length>1){
			//other parameters from the cmdline as key=value
			String[] paramArgs = new String[args.length-1];
			System.arraycopy(args, 1, paramArgs, 0, paramArgs.length);
			Map<String,String> paramMap = PropertyVariablesResolver.getParameters(paramArgs);
			return paramMap;
		}
		else return new HashMap<>();
	}

	protected Calendar getTermTime(){
		if(initialLifeTime<=0)return null;
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, initialLifeTime);
		return c;
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_JOBS;
	}
	
	@Override
	public String getType(){
		return "TargetSystemFactory";
	}

	@Override
	public String getServiceName(){
		return "TargetSystemFactory";
	}

	@Override
	public String getServiceDetails(Endpoint epr, UCCConfigurationProvider configurationProvider){
		try{
			IClientConfiguration securityProperties = configurationProvider.getClientConfiguration(epr.getUrl());
			SiteFactoryClient tsf = new SiteFactoryClient(epr, securityProperties, configurationProvider.getRESTAuthN());
			return getDescription(tsf);
		}catch(Exception ex){
			return "N/A ["+Log.getDetailMessage(ex)+"]";
		}
	}

	protected String getDescription(SiteFactoryClient sfc) throws Exception {
		JSONObject pr=sfc.getProperties();
		StringBuilder sb=new StringBuilder();
		String cr = System.getProperty("line.separator");
		sb.append("* Partitions").append(cr);
		JSONObject resources = pr.getJSONObject("resources");
		Iterator<String>qIter = resources.keys();
		while(qIter.hasNext()) {
			String partition = qIter.next();
			sb.append("   * ").append(partition).append(": ");
			JSONObject part = resources.getJSONObject(partition);
			List<String>keys = new ArrayList<>();
			keys.addAll(part.keySet());
			Collections.sort(keys);
			for(String r: keys) {
				String rVal = String.valueOf(part.get(r));
				sb.append("[").append(r);
				sb.append(": ").append(rVal);
				sb.append("] ");
			}sb.append(cr);
		}
		
		return sb.toString();
	}


	private static String lastTargetSystemAddress;

	public static String getLastTargetSystemAddress() {
		return lastTargetSystemAddress;
	}
	
	protected static void setLastTSSAddress(String lastAddress) {
		CreateTSS.lastTargetSystemAddress = lastAddress;
	}

}
