package eu.unicore.ucc.actions.job;

import org.apache.commons.cli.Option;

import eu.unicore.client.Job;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.util.UCCBuilder;

/**
 * Runs a single command through UNICORE
 * 
 * @author schuller
 */
public class Exec extends ActionBase {

	protected Runner runner;

	protected UCCBuilder builder;

	/**
	 * site to use
	 */
	protected String siteName=null;

	/**
	 * whether to actually submit the job - if <code>false</code>, brokering etc will
	 * be performed but no job will be submitted 
	 */
	protected boolean dryRun=false;

	/**
	 * whether to keep the finished job - if <code>false</code>, 
	 * the job will be deleted when done
	 */
	protected boolean keep=false;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Name of the site")
				.argName("Site")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_BROKER)
				.longOpt(OPT_BROKER_LONG)
				.desc("Use the specific named broker implementation (available: "+UCC.getBrokerList()+")")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_DRYRUN)
				.longOpt(OPT_DRYRUN_LONG)
				.desc("Dry run, do not submit the job")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_KEEP)
				.longOpt(OPT_KEEP_LONG)
				.desc("Don't remove finished job")
				.required(false)
				.build());
	}


	@Override
	public String getName() {
		return "exec";
	}

	@Override
	public String getArgumentList(){
		return "[<commands>]";
	}

	@Override
	public String getSynopsis(){
		return "Runs a command through UNICORE. " +
				"The command will not be run through a remote queue, but on the cluster login node. " +
				"The command and its arguments are taken from the UCC command line." +
				"UCC will wait for the job to finish and print standard output and error to the console.";
	}

	@Override
	public String getDescription(){
		return "run a command through UNICORE";
	}
	@Override
	public String getCommandGroup(){
		return "Job execution";
	}


	@Override
	public void process(){
		super.process();
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);
		dryRun=getBooleanOption(OPT_DRYRUN_LONG, OPT_DRYRUN);
		verbose("Dry run = "+dryRun);
		keep=getBooleanOption(OPT_KEEP_LONG, OPT_KEEP);
		verbose("Delete job when done = "+!keep);
		initBuilder(getCommandLine().getArgs());
		run();
	}

	protected void initBuilder(String[] args){
		try{
			builder=new UCCBuilder(registry, configurationProvider);
			builder.setMessageWriter(this);
			
			builder.setProperty("Output",output.getAbsolutePath());
			builder.setProperty("IDLocation",output.getAbsolutePath());
			builder.setProperty("KeepFinishedJob", String.valueOf(keep));
			builder.setProperty("DetailedStatusDisplay", "true");

			if(siteName!=null){
				builder.setProperty("Site", siteName);
			}
			
			Job job = new Job(builder.getJSON());
			
			if(args.length == 1)throw new IllegalArgumentException("Must specify a command");
			// first arg is command
			job.executable(args[1]);
			
			job.run_on_login_node();
			
			// add args
			if(args.length > 1 ){
				for(int i=2; i<args.length;i++){
					job.arguments(args[i]);
				}
			}
		}catch(Exception e){
			error("",e);
			endProcessing(ERROR_CLIENT);
		}

	}

	protected void run(){
		runner=new Runner(registry,configurationProvider,builder,this);
		runner.setAsyncMode(false);
		runner.setBriefOutfileNames(true);
		runner.setOutputToConsole(true);
		runner.setDryRun(dryRun);
		runner.setProperties(properties);
		String brokerName = getOption(OPT_BROKER_LONG, OPT_BROKER);
		if(siteName!=null){
			brokerName = "LOCAL";
		}
		runner.setBroker(UCC.getBroker(brokerName, this));
		try{
			runner.run();
		}catch(RuntimeException ex){
			runner.dumpJobLog();
			error("Failed to execute remote command", ex);
			endProcessing(ERROR);
		}
	}

}