package eu.unicore.ucc.actions;

import org.apache.commons.cli.Option;

import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.lookup.Connector;

/**
 * "Connect" user to the sites. Will create TSSs if none found
 * 
 * @author schuller
 */
public class Connect extends ActionBase {

	// the initial lifetime (in days) for newly created TSSs
	private int initialLifeTime;

	private static String lastRegistryURL;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_LIFETIME)
				.longOpt(OPT_LIFETIME_LONG)
				.desc("Initial lifetime (in days) for created target systems.")
				.argName("Lifetime")
				.hasArg()
				.required(false)
				.build());
	}

	@Override
	public String getName(){
		return "connect";
	}

	@Override
	public String getSynopsis(){
		return "Connects to UNICORE. If not yet done, target system services are initialised.";
	}

	@Override
	public String getDescription(){
		return "connect to UNICORE";
	}

	@Override
	public void process() throws Exception {
		super.process();
		lastRegistryURL=registryURL;
		initialLifeTime=getNumericOption(OPT_LIFETIME_LONG, OPT_LIFETIME, -1);
		if(initialLifeTime>0){
			console.verbose("New TSSs will have a lifetime of <{}> days.", initialLifeTime);
		}else{
			console.verbose("Using site default for TSS lifetime.");
		}
		Connector c = new Connector(registry, configurationProvider);
		c.setBlacklist(blacklist);
		c.run();
		int tsfAvailable=c.getAvailableTSF();
		if(tsfAvailable==0)console.info("There are no target system factories in the selected registry.");
		int tssAvailable=c.getAvailableTSS();
		console.info("You can access {} target system(s).", tssAvailable);
		//it should be considered an error if no sites are available
		if(tssAvailable==0) {
			throw new Exception("No sites available!");
		}
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_JOBS;
	}

	static String getLastReqistryURL(){
		return lastRegistryURL;
	}
	
	public IRegistryClient getRegistry(){
		return registry;
	}

	public UCCConfigurationProvider getConfigProvider(){
		return configurationProvider;
	}
}
