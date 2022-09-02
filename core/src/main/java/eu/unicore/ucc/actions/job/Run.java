package eu.unicore.ucc.actions.job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;

import org.apache.commons.cli.Option;

import de.fzj.unicore.uas.util.UnitParser;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.AllocationClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.helpers.EndProcessingException;
import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.util.UCCBuilder;

/**
 * runs a job through UNICORE
 * 
 * @author schuller
 */
public class Run extends ActionBase {

	protected Runner runner;

	protected UCCBuilder builder;

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
	
	/**
	 * quiet mode (e.g. don't write job id files)
	 */
	protected boolean quiet = false;

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
		getOptions().addOption(Option.builder(OPT_QUIET)
				.longOpt(OPT_QUIET_LONG)
				.desc("Quiet mode, don't write job ID file")
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
	public void process(){
		super.process();
		boolean success = true;
		if(getBooleanOption(OPT_SAMPLE_LONG, OPT_SAMPLE)){
			printSampleJob();
			endProcessing(0);
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

		quiet = getBooleanOption(OPT_QUIET_LONG, OPT_QUIET);
		verbose("Quiet mode = " + quiet);

		tags = getCommandLine().getOptionValues(OPT_TAGS);
		if(tags!=null) {
			verbose("Job tags = " + Arrays.deepToString(tags));
		}
		if(getCommandLine().getArgs().length>1){
			for(int i=1; i<getCommandLine().getArgs().length;i++){
				initBuilder(getCommandLine().getArgs()[i]);
				success = success && run()==0;
			}
		}
		else{
			initBuilderFromStdin();
			success = run()==0;
		}
		if(!success) {
			throw new EndProcessingException(1, "Job(s) failed.");
		}
	}

	protected void initBuilder(String jobFileName){
		try{
			File jobFile = new File(jobFileName);
			builder = new UCCBuilder(jobFile, registry, configurationProvider);
			builder.setMessageWriter(this);
			verbose("Read job from <"+jobFileName+">");
		}catch(Exception e){
			error("Can't parse job file <"+jobFileName+">",e);
			endProcessing(ERROR_CLIENT);
		}
		configureBuilder();
	}

	protected void initBuilderFromStdin(){
		try{
			message("Reading job from stdin:");
			message("");
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			int b=0;
			while((b=System.in.read())!=-1){
				bos.write(b);
			}
			builder = new UCCBuilder(bos.toString(), registry, configurationProvider);
			configureBuilder();
		}catch(Exception e){
			error("Can't read job from stdin.",e);
			endProcessing(ERROR_CLIENT);
		}
	}

	protected void configureBuilder(){
		builder.setProperty("Output",output.getAbsolutePath());
		builder.setProperty("IDLocation",output.getAbsolutePath());
		builder.setProperty("KeepFinishedJob", "true");
		builder.setProperty("DetailedStatusDisplay", "true");
		if(scheduled!=null)builder.setProperty("Not before", scheduled);
		String siteFromJob = builder.getSite();
		if(siteName!=null){
			builder.setProperty("Site", siteName);
		}
		else{
			// commandline option overrides
			siteName = siteFromJob;
		}
		if(tags!=null&&tags.length>0) {
			builder.addTags(tags);
		}
	}
	
	protected int run(){
		runner=new Runner(registry,configurationProvider,builder,this);
		runner.setAsyncMode(!synchronous);
		runner.setQuietMode(quiet);
		runner.setBriefOutfileNames(brief);
		runner.setDryRun(dryRun);
		runner.setProperties(properties);
		String brokerName = getOption(OPT_BROKER_LONG, OPT_BROKER);
		if(siteName!=null){
			brokerName = "LOCAL";
		}
		runner.setBroker(UCC.getBroker(brokerName, this));
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
				lastJobPropertiesFile=runner.dumpJobProperties();
				lastJobAddress=runner.getJob().getEndpoint().getUrl();
				lastJobFile=builder.getProperty("jobIdFile");
				try{
					lastJobDirectory=runner.getJob().getLinkUrl("workingDirectory");
				}catch(Exception ex){}
			}
		}catch(RuntimeException ex){
			runner.dumpJobLog();
			error("Failed to run job",ex);
			endProcessing(ERROR);
		}
		try {
			if(synchronous && !Status.SUCCESSFUL.equals(runner.getStatus())) {
				return ERROR;
			}
		}catch(Exception ex) {}
		return 0;
	}

	public void printSampleJob(){
		message(UCCBuilder.getJobSample());
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

	private static String lastJobPropertiesFile;

	public static String getLastJobPropertiesFile() {
		return lastJobPropertiesFile;
	}
}
