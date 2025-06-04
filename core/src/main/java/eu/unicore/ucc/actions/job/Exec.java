package eu.unicore.ucc.actions.job;

import java.util.Arrays;

import org.apache.commons.cli.Option;

import eu.unicore.client.Endpoint;
import eu.unicore.client.Job;
import eu.unicore.client.core.AllocationClient;
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

	private Runner runner;

	private UCCBuilder builder;

	private String siteName;

	private String loginNode;

	private boolean dryRun = false;

	private boolean keepFinishedJob = false;

	private String[] tags;

	private boolean asynchronous;

	// Allocation job URL
	private String allocation = null;

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
		getOptions().addOption(Option.builder(OPT_LOGIN_NODE)
				.longOpt(OPT_LOGIN_NODE_LONG)
				.desc("Login node to use")
				.argName("LoginNode")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_ALLOCATION)
				.longOpt(OPT_ALLOCATION_LONG)
				.desc("Allocation URL")
				.required(false)
				.argName("Allocation")
				.hasArg()
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
		getOptions().addOption(Option.builder(OPT_MODE)
				.longOpt(OPT_MODE_LONG)
				.desc("Run asynchronous, don't wait for finish, don't get results.")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_TAGS)
				.longOpt(OPT_TAGS_LONG)
				.desc("Tag the job with the given tag(s) (comma-separated)")
				.required(false)
				.hasArgs()
				.valueSeparator(',')
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
	public void process() throws Exception {
		super.process();
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);
		allocation = getCommandLine().getOptionValue(OPT_ALLOCATION);
		if(allocation!=null && siteName!=null) {
			throw new IllegalArgumentException("Cannot have both 'allocation' and 'sitename' arguments.");
		}
		loginNode=getCommandLine().getOptionValue(OPT_LOGIN_NODE);
		dryRun=getBooleanOption(OPT_DRYRUN_LONG, OPT_DRYRUN);
		console.verbose("Dry run = {}", dryRun);
		keepFinishedJob=getBooleanOption(OPT_KEEP_LONG, OPT_KEEP);
		console.verbose("Delete job when done = {}", !keepFinishedJob);
		asynchronous=getBooleanOption(OPT_MODE_LONG, OPT_MODE);
		console.verbose("Asynchronous processing = {}", asynchronous);
		tags = getCommandLine().getOptionValues(OPT_TAGS);
		if(tags!=null) {
			console.verbose("Job tags = {}", Arrays.deepToString(tags));
		}
		initBuilder(getCommandLine().getArgs());
		run();
	}

	private void initBuilder(String[] args) throws Exception {
		builder = new UCCBuilder(registry, configurationProvider);
		builder.setProperty("_ucc_Output",output.getAbsolutePath());
		builder.setProperty("_ucc_KeepFinishedJob", String.valueOf(keepFinishedJob));
		builder.setProperty("_ucc_DetailedStatusDisplay", "true");
		if(tags!=null&&tags.length>0) {
			builder.addTags(tags);
		}
		builder.setSite(siteName);
		Job job = new Job(builder.getJSON());
		if(args.length == 1)throw new IllegalArgumentException("Must specify a command");
		// first arg is command
		job.executable(args[1]);
		job.run_on_login_node(loginNode);
		if(args.length > 1 ){
			for(int i=2; i<args.length;i++){
				job.arguments(args[i]);
			}
		}
	}

	private void run(){
		runner = new Runner(registry,configurationProvider,builder);
		runner.setAsyncMode(asynchronous);
		runner.setBriefOutfileNames(true);
		runner.setOutputToConsole(true);
		runner.setDryRun(dryRun);
		runner.setProperties(properties);
		String brokerName = getOption(OPT_BROKER_LONG, OPT_BROKER);
		if(siteName!=null){
			brokerName = "LOCAL";
		}
		runner.setBroker(UCC.getBroker(brokerName));
		if(allocation!=null) {
			try {
				AllocationClient ac = new AllocationClient(new Endpoint(allocation),
						configurationProvider.getClientConfiguration(allocation),
						configurationProvider.getRESTAuthN());
				runner.setSubmissionService(ac);
			}catch(Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		runner.run();
	}
}
