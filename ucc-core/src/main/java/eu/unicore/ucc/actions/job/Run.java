package eu.unicore.ucc.actions.job;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.commons.cli.OptionBuilder;

import de.fzj.unicore.uas.util.UnitParser;
import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.helpers.EndProcessingException;
import de.fzj.unicore.ucc.util.UCCBuilder;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.runner.Runner;

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
	 * synchronous mode
	 */
	protected boolean synchronous;

	/**
	 * do not add job id prefixes to output file names 
	 */
	protected boolean brief;

	/**
	 * whether the JSDL doc should be validated
	 */
	protected boolean validateJSDL=true;

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
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_MODE_LONG)
				.withDescription("Run asynchronous, writing a job ID file for use with other ucc commands")
				.isRequired(false)
				.create(OPT_MODE)
				);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SITENAME_LONG)
				.withDescription("Site Name")
				.withArgName("Vsite")
				.hasArg()
				.isRequired(false)
				.create(OPT_SITENAME)
				);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_NOPREFIX_LONG)
				.withDescription("Short output file names")
				.isRequired(false)
				.create(OPT_NOPREFIX)
				);
	
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SAMPLE_LONG)
				.withDescription("Print an example job and quit")
				.isRequired(false)
				.create(OPT_SAMPLE)
				);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SCHEDULED_LONG)
				.withDescription("Schedule the job for a specific time (in ISO8601 format)")
				.isRequired(false)
				.hasArg()
				.create(OPT_SCHEDULED)
				);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_BROKER_LONG)
				.withDescription("Use the specific named broker implementation (available: "+UCC.getBrokerList()+")")
				.isRequired(false)
				.hasArg()
				.create(OPT_BROKER)
				);
		
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_TAGS_LONG)
				.withDescription("Tag the job with the given tag(s)")
				.isRequired(false)
				.hasArg()
				.create(OPT_TAGS)
				);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_QUIET_LONG)
				.withDescription("Quiet mode, don't write job ID file")
				.isRequired(false)
				.create(OPT_QUIET)
				);
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

		String tagsArg = getCommandLine().getOptionValue(OPT_TAGS);
		if(tagsArg!=null) {
			tags = tagsArg.split(",");
			verbose("Job tags = " + tagsArg);
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
