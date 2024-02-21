package eu.unicore.ucc.actions.info;

import java.util.List;

import org.apache.commons.cli.Option;

import eu.unicore.client.Endpoint;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.ucc.Command;
import eu.unicore.ucc.IServiceInfoProvider;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.actions.shell.URLCompleter;

/**
 * print info about services available in the registry
 */
public class SystemInfo extends ActionBase {

	private boolean details=false;
	
	private String pattern;
	
	private static final String OPT_PATTERN_LONG = "url-pattern";
	private static final String OPT_PATTERN = "P";
	
	private static final String OPT_RAW_LONG = "raw";
	private static final String OPT_RAW = "R";
	
	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_DETAILED)
				.longOpt(OPT_DETAILED_LONG)
				.desc("Detailed output")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_RAW)
				.longOpt(OPT_RAW_LONG)
				.desc("Show raw registry content")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_PATTERN)
				.longOpt(OPT_PATTERN_LONG)
				.desc("Only show details for endpoint URLs matching "
						+ "the given regexp (e.g. \".*/storage.*\"")
				.required(false)
				.hasArg()
				.build());	
	}
	
	@Override
	public String getName() {
		return "system-info";
	}
	
	
	@Override
	public String getDescription() {
		return "check the availability of services";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_UTILITY;
	}
	
	@Override
	public String getSynopsis() {
		return "Checks the registry for the availability of UNICORE endpoints. " +
				"If the '-l' option is used, the address of the found endpoints is printed";
	}


	@Override
	public void process() {
		super.process();
		details=getBooleanOption(OPT_DETAILED_LONG, OPT_DETAILED);
		pattern = getOption(OPT_PATTERN_LONG, OPT_PATTERN, null);
		if(getBooleanOption(OPT_RAW_LONG, OPT_RAW)){
			printRawContent();
		}
		else{
			getInfo();
		}
	}
	
	protected void getInfo(){
		for(Command c: UCC.getAllCommands()){
			if(c instanceof IServiceInfoProvider){
				getInfo((IServiceInfoProvider)c);
			}
		}
	}
	
	protected void getInfo(IServiceInfoProvider info){
		String type = info.getType();
		String name = info.getServiceName();
		message("");
		message("Checking for <"+name+"> endpoint ...");
		try{
			List<Endpoint> list = registry.listEntries(new RegistryClient.ServiceTypeFilter(type));
			int n=list.size();
			if(n==0){
				message("... no endpoints available");
			}
			else{
				message("... OK, found "+n+" endpoint(s)");
				for(Endpoint e: list){
					String url = e.getUrl();
					if(isBlacklisted(url) || (pattern!=null && !url.matches(pattern))) {
						continue;
					}
					URLCompleter.registerSiteURL(url);
					if(details) {
						message(" * "+url);
						String infoS=info.getServiceDetails(e,configurationProvider);
						if(infoS!=null)message("  "+infoS);
					}
				}
			}
		}catch(Exception e){
			error("... FAILED.",e);
		}

	}	

	protected void printRawContent() {
		try{
			for(Endpoint e: registry.listEntries()){
				message("Entry:           "+e.getUrl());
				message("Server identity: "+e.getServerIdentity());
				message("Service type:    "+e.getInterfaceName());
			}
		} catch(Exception ex){
			error("... FAILED.",ex);
		}
	}

}
