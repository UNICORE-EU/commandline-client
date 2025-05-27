package eu.unicore.ucc.actions.job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.io.IOUtils;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.AllocationClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;

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

	Status waitFor = null;

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
		getOptions().addOption(Option.builder(OPT_WAIT)
				.longOpt(OPT_WAIT_LONG)
				.desc("(Async mode) wait for the given job status ("+waitableJobStatuses+") before exiting.")
				.hasArg(true)
				.required(false)
				.build());
	}

	@Override
	public String getName() {
		return "run";
	}

	@Override
	public String getArgumentList(){
		return "[<jobfile(s)>]";
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
		return CMD_GRP_JOBS;
	}

	@Override
	public Collection<String> getAllowedOptionValues(String option) {
		if(OPT_WAIT.equals(option)) {
			return waitableJobStatuses;
		}
		return null;
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
			throw new IllegalArgumentException("Cannot have both '--"
					+OPT_ALLOCATION_LONG+"' and '--"+OPT_SITENAME_LONG+"' arguments.");
		}
		synchronous=!getBooleanOption(OPT_MODE_LONG, OPT_MODE);
		console.verbose("Synchronous processing = {}", synchronous);
		brief=getBooleanOption(OPT_NOPREFIX_LONG, OPT_NOPREFIX);
		console.verbose("Adding job id to output file names = {}", !brief);
		scheduled=getOption(OPT_SCHEDULED_LONG, OPT_SCHEDULED);
		if(scheduled!=null){
			scheduled=UnitParser.convertDateToISO8601(scheduled);
			console.verbose("Will schedule job submission for {}", scheduled);
		}
		dryRun=getBooleanOption(OPT_DRYRUN_LONG, OPT_DRYRUN);
		console.verbose("Dry run = "+dryRun);
		multiThreaded = getBooleanOption("multi-threaded", "J");
		console.verbose("Multi threaded = {}", multiThreaded);
		tags = getCommandLine().getOptionValues(OPT_TAGS);
		if(tags!=null) {
			console.verbose("Job tags = {}", Arrays.deepToString(tags));
		}
		String waitForSpec = getOption(OPT_WAIT_LONG, OPT_WAIT);
		if(waitForSpec!=null) {
			if(synchronous) {
				throw new IllegalArgumentException("Option '--"+
						OPT_WAIT_LONG+"' requires '--"+OPT_MODE_LONG+"'");
			}
			try{
				waitFor = Status.valueOf(waitForSpec);
				if(waitFor==Status.FAILED || waitFor==Status.UNDEFINED) {
					throw new Exception();
				}
			}catch(Exception ex) {
				throw new IllegalArgumentException("'--"+OPT_WAIT_LONG
						+"' accepts one of: "+Arrays.asList(waitableJobStatuses));
			}
		}
		final List<String>errors = Collections.synchronizedList(new ArrayList<>());
		if(getCommandLine().getArgs().length>1){
			List<Thread> threads = new ArrayList<>();
			for(int i=1; i<getCommandLine().getArgs().length;i++){
				final String jobFile = getCommandLine().getArgs()[i];
				if(multiThreaded) {
					Thread t = new Thread(()->{
						Pair<Integer,String> result = null;
						try {
							result = run(readJob(jobFile));
							if(result.getM1()!=0) {
								errors.add("ERROR running <"+jobFile+">: "+result.getM2());
							}
						}catch(Exception ex) {
							errors.add(Log.createFaultMessage("Job failed for <"+jobFile+">", ex));
						}
					});
					t.setName("[ucc run "+i+"]");
					threads.add(t);
					t.start();
				}
				else {
					Pair<Integer,String> result = run(readJob(jobFile));
					if(result.getM1()!=0) {
						errors.add("ERROR running <"+jobFile+">: "+result.getM2());
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
			Pair<Integer,String> result = run(readJob());
			if(result.getM1()!=0) {
				errors.add("ERROR: "+result.getM2());
			}
		}
		if(errors.size()>0) {
			for(String s: errors) {
				console.verbose("{}", s);
			}
			throw new Exception("Job(s) failed.");
		}
	}

	protected UCCBuilder readJob(String jobFileName) throws Exception {
		File jobFile = new File(jobFileName);
		UCCBuilder builder = new UCCBuilder(jobFile, registry, configurationProvider);
		console.verbose("Read job from <{}>", jobFileName);
		configureBuilder(builder);
		return builder;
	}

	protected UCCBuilder readJob() throws Exception {
		console.info("Reading job from stdin:");
		console.info("");
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
		builder.setProperty("_ucc_Output",output.getAbsolutePath());
		builder.setProperty("_ucc_KeepFinishedJob", "true");
		builder.setProperty("_ucc_DetailedStatusDisplay", "true");
		if(scheduled!=null)builder.setProperty("Not before", scheduled);
		builder.setSite(siteName);
		if(tags!=null&&tags.length>0) {
			builder.addTags(tags);
		}
	}

	protected Pair<Integer, String> run(UCCBuilder builder){
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
					lastJobFile=builder.getProperty("_ucc_jobIdFile");
					if(waitFor!=null) {
						console.verbose("Waiting for job to be {} ...", waitFor);
						runner.getJob().poll(waitFor);
					}
				}
				try{
					lastJobDirectory=runner.getJob().getLinkUrl("workingDirectory");
					properties.put(PROP_LAST_JOBDIR_URL, lastJobDirectory);
				}catch(Exception ex){}
			}
		}catch(Exception ex){
			return new Pair<>(ERROR, Log.createFaultMessage("Error processing job", ex));
		}
		try {
			if(Status.FAILED.equals(runner.getJob().getStatus())) {
				return new Pair<>(ERROR, runner.getJob().getStatusMessage());
			}
		}catch(Exception ex) {}
		return new Pair<>(0, "");
	}

	public void printSampleJob(){
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(
				"META-INF/examples/basic.json")){
			console.info("Example job:\n{}",IOUtils.toString(is, "UTF-8"));
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
		console.info("For a full description, see:");
		console.info("https://unicore-docs.readthedocs.io/en/latest/user-docs/rest-api/job-description/index.html");
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
