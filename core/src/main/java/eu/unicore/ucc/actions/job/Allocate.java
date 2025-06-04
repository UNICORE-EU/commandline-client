package eu.unicore.ucc.actions.job;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Option;

import eu.unicore.client.Job;
import eu.unicore.client.Job.Type;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.util.UCCBuilder;

/**
 * Allocates resources on the batch system via UNICORE
 *
 * @author schuller
 */
public class Allocate extends ActionBase {

	private Runner runner;

	private UCCBuilder builder;

	private String siteName;

	private boolean dryRun = false;

	private String[] tags;

	private boolean asynchronous;

	private final Map<String,String>resourceRequests = new HashMap<>();

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
		getOptions().addOption(Option.builder(OPT_MODE)
				.longOpt(OPT_MODE_LONG)
				.desc("Only submit, don't wait for the allocation to start running.")
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

	private void readResources(){
		resourceRequests.clear();
		int length=getCommandLine().getArgs().length;
		if(length<1)return;
		for(int i=1; i<length; i++){
			String p=getCommandLine().getArgs()[i];
			String[]split=p.split("=");
			String key=split[0];
			String value=split[1];
			console.verbose("Have resource request: {}={}", key, value);
			resourceRequests.put(key, value);
		}
		lastParams = resourceRequests;
	}

	@Override
	public String getName() {
		return "allocate";
	}

	@Override
	public String getArgumentList(){
		return "allocate [<options>] [resource1=value1 resource2=value2 ...]";
	}

	@Override
	public String getSynopsis(){
		return "This will create a batch system allocation, if the system supports that."
				+ "Once the allocation is running, use the allocation ID to submit jobs more "
				+ "'interactively', without having to wait for them to get scheduled.";
	}

	@Override
	public String getDescription(){
		return "allocate resources through UNICORE";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_JOBS;
	}

	@Override
	public void process() throws Exception {
		super.process();
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);
		dryRun=getBooleanOption(OPT_DRYRUN_LONG, OPT_DRYRUN);
		console.verbose("Dry run = {}", dryRun);
		asynchronous=getBooleanOption(OPT_MODE_LONG, OPT_MODE);
		console.verbose("Asynchronous processing = {}", asynchronous);
		tags = getCommandLine().getOptionValues(OPT_TAGS);
		if(tags!=null) {
			console.verbose("Job tags = {}", Arrays.deepToString(tags));
		}
		readResources();
		initBuilder();
		run();
	}

	private void initBuilder() throws Exception {
		builder = new UCCBuilder(registry, configurationProvider);
		builder.setProperty("_ucc_Output",output.getAbsolutePath());
		builder.setProperty("_ucc_DetailedStatusDisplay", "true");
		if(tags!=null&&tags.length>0) {
			builder.addTags(tags);
		}
		builder.setSite(siteName);
		Job job = new Job(builder.getJob());
		job.type(Type.ALLOCATE);
		for(String rName: resourceRequests.keySet()) {
			job.resources().other(rName, resourceRequests.get(rName));
		}
	}

	private void run() throws Exception {
		runner = new Runner(registry,configurationProvider,builder);
		runner.setAsyncMode(true);
		runner.setBriefOutfileNames(true);
		runner.setOutputToConsole(true);
		runner.setDryRun(dryRun);
		runner.setProperties(properties);
		String brokerName = getOption(OPT_BROKER_LONG, OPT_BROKER);
		if(siteName!=null){
			brokerName = "LOCAL";
		}
		runner.setBroker(UCC.getBroker(brokerName));
		runner.run();
		if(!asynchronous && !dryRun) {
			// make sure job is "RUNNING"
			JobClient job = runner.getJob();
			console.verbose("Waiting for allocation job to be RUNNING...");
			job.poll(Status.RUNNING);
			console.verbose("Allocation job is {}", job.getStatus());
		}
	}

	// for unit testing
	public static Map<String,String>lastParams;
}
