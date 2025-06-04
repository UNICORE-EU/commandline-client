package eu.unicore.ucc.actions.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;

import eu.unicore.client.Endpoint;
import eu.unicore.client.admin.AdminServiceClient;
import eu.unicore.client.admin.AdminServiceClient.AdminCommand;
import eu.unicore.client.admin.AdminServiceClient.Result;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.ucc.actions.ActionBase;

public class RunCommand extends ActionBase {

	public static final String OPT_URL_LONG="url";
	public static final String OPT_URL="u";

	private String siteName;
	private String url;
	private String cmd;

	private final Map<String,String>params = new HashMap<>();

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Site name")
				.argName("Site")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_URL)
				.longOpt(OPT_URL_LONG)
				.desc("Admin service URL")
				.argName("URL")
				.hasArg()
				.required(false)
				.build());
	}

	@Override
	public void process() throws Exception {
		super.process();
		int length=getCommandLine().getArgs().length;
		if(length<2){
			throw new IllegalArgumentException("You must provide at least a command name as argument.");
		}
		params.clear();
		cmd=getCommandLine().getArgs()[1];
		for(int i=2; i<length; i++){
			String p=getCommandLine().getArgs()[i];
			String[]split=p.split("=", 2);
			if(split.length<2) {
				throw new IllegalArgumentException("'"+p+"': format must be key=value");
			}
			String key=split[0];
			String value=split[1];
			console.verbose("Have parameter: {}={}", key, value);
			params.put(key, value);
		}
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);
		url=getCommandLine().getOptionValue(OPT_URL);
		if(siteName==null && url==null){
			//try to guess URL from the registry URL
			if(registryURL!=null){
				try{
					String regurl = ((RegistryClient)registry).getEndpoint().getUrl();
					url = regurl.replace("registries/default_registry", "admin");
					console.verbose("Will try fallback admin service URL: <{}>", url);
				}catch(Exception ex){}
			}
			if(url==null){
				throw new IllegalArgumentException("Either URL or site name must be given!");
			}
		}
		if(siteName!=null && url!=null){
			throw new IllegalArgumentException("URL and site name cannot both be given!");
		}
		AdminServiceClient asc=null;
		List<AdminCommand>availableCmds = null;
		asc = createClient();
		console.verbose("Contacted admin service at <{}>", url);
		availableCmds = asc.getCommands();
		//check command availability
		boolean haveCmd = false;
		for(AdminCommand c: availableCmds){
			if(cmd.equals(c.name)) {
				haveCmd = true;
				break;
			}
		}
		if(!haveCmd) {
			throw new IllegalArgumentException("No such command: <"+cmd+">");
		}
		Result result = asc.runCommand(cmd, params);
		if(result.successful){
			console.info("SUCCESS, service reply: {}", result.message);
			if(result.results.size()>0){
				console.info("{}", result.results);
			}
		}else{
			console.info("Action was NOT SUCCESSFUL, service reply: {}", result.message);
		}
	}

	private AdminServiceClient createClient()throws Exception{
		if(url==null){
			findURL();
		}
		AdminServiceClient asc = new AdminServiceClient(new Endpoint(url),
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
		return asc;
	}

	private void findURL()throws Exception{
		List<Endpoint> tsfs = registry.listEntries(new RegistryClient.ServiceTypeFilter("TargetSystemFactory"));
		for(Endpoint epr: tsfs){
			String tsfURL = epr.getUrl();
			if(tsfURL.contains("/"+siteName+"/")){
				int endIndex = tsfURL.lastIndexOf("/core/factories/");
				url = tsfURL.substring(0, endIndex)+"/admin";
			}	
		}
	}

	@Override
	public String getName() {
		return "admin-runcommand";
	}

	@Override
	public String getArgumentList() {
		return "<command> [key1=value1 key2=value2 ...]";
	}

	@Override
	public String getSynopsis() {
		return "Runs a server-side administrative command, which can be parametrised, " 
				+"and displays the results. The mandatory '-s' option is used to select the site.";
	}

	@Override
	public String getDescription() {
		return "run a server-side administrative command";
	}

	@Override
	public String getCommandGroup() {
		return CMD_GRP_ADMIN;
	}
}
