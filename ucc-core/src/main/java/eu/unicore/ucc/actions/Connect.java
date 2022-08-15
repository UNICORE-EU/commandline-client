package eu.unicore.ucc.actions;

import org.apache.commons.cli.Option;

import eu.unicore.ucc.lookup.Connector;

/**
 * "Connect" user to the sites. Will create TSSs if none found
 * 
 * @author schuller
 */
public class Connect extends ActionBase {
	/**
	 * the initial lifetime (in days) for newly created TSSs
	 */
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
	public String getArgumentList(){
		return "";
	}

	@Override
	public String getSynopsis(){
		return "Connects to UNICORE. " +
		"If not yet done, target system services are initialised.";
	}
	@Override
	public String getDescription(){
		return "connect to UNICORE";
	}

	@Override
	public void process() {
		super.process();

		lastRegistryURL=registryURL;
		
		initialLifeTime=getNumericOption(OPT_LIFETIME_LONG, OPT_LIFETIME, -1);
		if(initialLifeTime>0){
			verbose("New TSSs will have a lifetime of <"+initialLifeTime+"> days.");
		}else{
			verbose("Using site default for TSS lifetime.");
		}

		Connector c=new Connector(registry, configurationProvider, this);
		c.setBlacklist(blacklist);
		c.run();
		int tsfAvailable=c.getAvailableTSF();
		if(tsfAvailable==0)message("There are no target system factories in the selected registry.");
		int tssAvailable=c.getAvailableTSS();
		message("You can access "+tssAvailable+" target system(s).");
		//it should be considered an error if no sites are available
		if(tssAvailable==0)endProcessing(ERROR);
	}

	@Override
	public String getCommandGroup(){
		return "General";
	}

	static String getLastReqistryURL(){
		return lastRegistryURL;
	}
}
