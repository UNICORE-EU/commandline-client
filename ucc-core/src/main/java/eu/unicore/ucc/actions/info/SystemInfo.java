package eu.unicore.ucc.actions.info;

import java.util.List;

import org.apache.commons.cli.OptionBuilder;

import de.fzj.unicore.ucc.Command;
import de.fzj.unicore.ucc.IServiceInfoProvider;
import de.fzj.unicore.ucc.UCC;
import eu.unicore.client.Endpoint;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.ucc.actions.ActionBase;

/**
 * print info about services available in the registry
 */
public class SystemInfo extends ActionBase {

	private boolean details=false;
	
	private boolean raw=false;
	
	private String pattern;
	
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt("long")
				.withDescription("Detailed output.")
				.isRequired(false)
				.create("l")
				);
		
		getOptions().addOption(OptionBuilder.withLongOpt("raw")
				.withDescription("Show raw registry content.")
				.isRequired(false)
				.create("R")
				);
		getOptions().addOption(OptionBuilder.withLongOpt("url-pattern")
				.withDescription("Only show details for endpoint URLs matching "
						+ "the given regexp (e.g. \".*/storage.*\"")
				.hasArg()
				.isRequired(false)
				.create("P")
				);
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
		return "General";
	}
	
	@Override
	public String getSynopsis() {
		return "Checks the registry for the availability of UNICORE endpoints. " +
				"If the '-l' option is used, the address of the found endpoints is printed";
	}


	@Override
	public void process() {
		super.process();
		details=getBooleanOption("long", "l");
		raw=getBooleanOption("raw", "R");
		pattern = getOption("pattern", "P", null);
		if(raw){
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
				if(details){
					for(Endpoint e: list){
						String url = e.getUrl();
						if(isBlacklisted(url) || (pattern!=null && !url.matches(pattern))) {
							continue;
						}
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
