package eu.unicore.ucc.actions.job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.Option;
import org.apache.commons.io.IOUtils;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.AllocationClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.UCCException;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.util.UCCBuilder;

/**
 * runs a job through UNICORE
 * 
 * @author schuller
 */
public class Run extends ActionBase {

	/**
	 * site to use
	 */
	protected String siteName=null;

	/**
	 * Allocation job URL
	 */
	protected String allocation=null;

	/**
	 * synchronous mode
	 */
	protected boolean synchronous;

	/**
	 * do not add job id prefixes to output file names 
	 */
	protected boolean brief;

	/**
	 * whether job submission to the batch system (server-side) should be
	 * scheduled for a certain time 
	 */
	protected String scheduled=null;

	/**
	 * whether to actually submit the job - if <code>false</code>, brokering etc will
	 * be performed but no job will be submitted 
	 */
	protected boolean dryRun=false;

	protected boolean multiThreaded=false;

	protected String[] tags;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_MODE)
				.longOpt(OPT_MODE_LONG)
				.desc("Run asynchronous, writing a job ID file for use with other ucc commands")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Site Name")
				.required(false)
				.argName("Site")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_ALLOCATION)
				.longOpt(OPT_ALLOCATION_LONG)
				.desc("Allocation URL")
				.required(false)
				.argName("Allocation")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_NOPREFIX)
				.longOpt(OPT_NOPREFIX_LONG)
				.desc("Short output file names")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_SAMPLE)
				.longOpt(OPT_SAMPLE_LONG)
				.desc("Print an example job and quit")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_SCHEDULED)
				.longOpt(OPT_SCHEDULED_LONG)
				.desc("Schedule the job for a specific time (in ISO8601 format)")
				.required(false)
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_BROKER)
				.longOpt(OPT_BROKER_LONG)
				.desc("Use the specific named broker implementation (available: "+UCC.getBrokerList()+")")
				.required(false)
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_TAGS)
				.longOpt(OPT_TAGS_LONG)
				.desc("Tag the job with the given tag(s) (comma-separated)")
				.required(false)
				.hasArgs()
				.valueSeparator(',')
				.build());
		getOptions().addOption(Option.builder(OPT_DRYRUN)
				.longOpt(OPT_DRYRUN_LONG)
				.desc("Dry run, don't submit anything")
				.required(false)
				.build());
		getOptions().addOption(Option.builder("J")
				.longOpt("multi-threaded")
				.desc("Launch a thread for each job file to run.")
				.required(false)
				.build());
	}

	@Override
	public String getName() {
		return "run";
	}

	@Override
	public String getArgumentList(){
		return "[<jobfile>]";
	}

	@Override
	public String getSynopsis(){
		return "Runs a job through UNICORE. " +
				"The job definition is read from <jobfile> or stdin. " +
				"Run 'ucc run -"+OPT_SAMPLE+"' to see an example job."+
				"A job can be executed in two modes. In the default synchronous mode, " +
				"UCC will wait for the job to finish. In asynchonous mode, initiated by the '"+OPT_MODE+"' option, " +
				"the job will be submitted and started. Check status / retrieve output later with other UCC commands.";
	}

	@Override
	public String getDescription(){
		return "run a job through UNICORE";
	}
	@Override
	public String getCommandGroup(){
		return "Job execution";
	}


	@Override
	public void process() throws Exception {
		super.process();
		if(getBooleanOption(OPT_SAMPLE_LONG, OPT_SAMPLE)){
			printSampleJob();
			return;
		}
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);
		allocation = getCommandLine().getOptionValue(OPT_ALLOCATION);
		if(allocation!=null && siteName!=null) {
			throw new IllegalArgumentException("Cannot have both 'allocation' and 'sitename' arguments.");
		}
		synchronous=!getBooleanOption(OPT_MODE_LONG, OPT_MODE);
		verbose("Synchronous processing = "+synchronous);
		brief=getBooleanOption(OPT_NOPREFIX_LONG, OPT_NOPREFIX);
		verbose("Adding job id to output file names = "+!brief);
		scheduled=getOption(OPT_SCHEDULED_LONG, OPT_SCHEDULED);
		if(scheduled!=null){
			scheduled=UnitParser.convertDateToISO8601(scheduled);
			verbose("Will schedule job submission for "+scheduled);
		}
		dryRun=getBooleanOption(OPT_DRYRUN_LONG, OPT_DRYRUN);
		verbose("Dry run = "+dryRun);
		multiThreaded = getBooleanOption("multi-threaded", "J");
		verbose("Multi threaded = "+multiThreaded);
		tags = getCommandLine().getOptionValues(OPT_TAGS);
		if(tags!=null) {
			verbose("Job tags = " + Arrays.deepToString(tags));
		}
		final AtomicBoolean success = new AtomicBoolean(true);

		if(getCommandLine().getArgs().length>1){
			List<Thread> threads = new ArrayList<>();
			for(int i=1; i<getCommandLine().getArgs().length;i++){
				final String jobFile = getCommandLine().getArgs()[i];
				if(multiThreaded) {
					Thread t = new Thread(()->{
						try {
							if(run(readJob(jobFile))!=0) {
								success.set(false);
							}
						}catch(Exception ex) {
							success.set(false);
							error("Job failed for <"+jobFile+">", ex);
						}
					});
					t.setName("[ucc run "+i+"]");
					threads.add(t);
					t.start();
				}
				else {
					if(run(readJob(jobFile))!=0){
						success.set(false);
					}
				}
			}
			for(Thread t: threads) {
				try{
					t.join();
				}catch(InterruptedException ie) {}
			}
		}
		else{
			if(run(readJob())!=0){
				success.set(false);
			}
		}
		if(!success.get()) {
			throw new UCCException("Job(s) failed.");
		}
	}

	protected UCCBuilder readJob(String jobFileName) throws Exception {
		File jobFile = new File(jobFileName);
		UCCBuilder builder = new UCCBuilder(jobFile, registry, configurationProvider);
		verbose("Read job from <"+jobFileName+">");
		configureBuilder(builder);
		return builder;
	}

	protected UCCBuilder readJob() throws Exception {
		message("Reading job from stdin:");
		message("");
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		int b=0;
		while((b=System.in.read())!=-1){
			bos.write(b);
		}
		UCCBuilder builder = new UCCBuilder(bos.toString(), registry, configurationProvider);
		configureBuilder(builder);
		return builder;
	}

	protected void configureBuilder(UCCBuilder builder){
		builder.setProperty("Output",output.getAbsolutePath());
		builder.setProperty("KeepFinishedJob", "true");
		builder.setProperty("DetailedStatusDisplay", "true");
		if(scheduled!=null)builder.setProperty("Not before", scheduled);
		if(siteName!=null){
			builder.setProperty("Site", siteName);
		}
		if(tags!=null&&tags.length>0) {
			builder.addTags(tags);
		}
	}

	protected int run(UCCBuilder builder){
		Runner runner = new Runner(registry,configurationProvider,builder);
		runner.setAsyncMode(!synchronous);
		runner.setQuietMode(true);
		runner.setBriefOutfileNames(brief);
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
		try{
			runner.run();
			if(!dryRun){
				lastJobAddress=runner.getJob().getEndpoint().getUrl();
				if(!synchronous) {
					lastJobFile=builder.getProperty("jobIdFile");
				}
				try{
					lastJobDirectory=runner.getJob().getLinkUrl("workingDirectory");
				}catch(Exception ex){}
			}
		}catch(RuntimeException ex){
			runner.dumpJobLog();
			return ERROR;
		}
		try {
			if(synchronous && !Status.SUCCESSFUL.equals(runner.getStatus())) {
				return ERROR;
			}
		}catch(Exception ex) {}
		return 0;
	}

	public void printSampleJob(){
		message("Example job:");
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(
				"META-INF/examples/basic.json")){
			message(IOUtils.toString(is, "UTF-8"));
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
		message("For a full description, see:");
		message("https://unicore-docs.readthedocs.io/en/latest/user-docs/rest-api/job-description/index.html");
	}

	/*
	 * for unit testing
	 */
	private static String lastJobAddress;

	public static String getLastJobAddress() {
		return lastJobAddress;
	}

	private static String lastJobDirectory;

	public static String getLastJobDirectory() {
		return lastJobDirectory;
	}

	private static String lastJobFile;

	public static String getLastJobFile() {
		return lastJobFile;
	}

}
