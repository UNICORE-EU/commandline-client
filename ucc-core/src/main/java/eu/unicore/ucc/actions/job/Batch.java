package eu.unicore.ucc.actions.job;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.OptionBuilder;

import de.fzj.unicore.ucc.util.UCCBuilder;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.runner.RandomSelection;
import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.runner.RunningQueue;
import eu.unicore.ucc.runner.SiteSelectionStrategy;
import eu.unicore.ucc.runner.WeightedSelection;

/**
 * Batch processing of all jobs in a directory.
 * 
 * <p>
 * A "follow" mode waits for new jobs and executes them 
 * as they arrive. A number of switches and settings allow
 * customizing the behavior of this command, for example
 * setting the maximum number of concurrent jobs, etc.
 * </p>
 * 
 * @author schuller
 */
public class Batch extends ActionBase {

	private boolean follow;

	private File inputDir;

	private boolean keepJobs;

	private boolean noFetchOutcome;

	private boolean submitOnly;

	private String siteName;

	private RequestQueue requests;

	private RunningQueue running;

	private int numRequests=0;

	private SiteSelectionStrategy siteSelectionStragegy = new RandomSelection();

	/**
	 * default update interval
	 */
	public static final int DEFAULT_UPDATE=1000;

	/**
	 * default limit on number of running jobs
	 */
	public static final int DEFAULT_LIMIT=100;

	/**
	 * default limit on number of jobs submitted in one go
	 */
	public static final int DEFAULT_REQUEST_LIMIT=100;

	/**
	 * default number of executor threads
	 */
	public static final int DEFAULT_THREADS=4;

	//update interval for checking running jobs (in milliseconds)
	private int updateInterval;

	private int runningJobLimit;

	private int requestLimit=DEFAULT_REQUEST_LIMIT;

	private int numThreads;

	private boolean noResourceCheck=false;

	private ThreadPoolExecutor executor = null;

	private final AtomicInteger activeJobs=new AtomicInteger(0);

	private final AtomicInteger activeRequests=new AtomicInteger(0);

	private volatile boolean isShutdown=false;

	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_FOLLOW_LONG)
				.withDescription("Follow mode")
				.isRequired(false)
				.create(OPT_FOLLOW)
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_INPUTDIR_LONG)
				.withDescription("Input directory")
				.withArgName("Inputdir")
				.hasArg()
				.isRequired(true)
				.create(OPT_INPUTDIR)
		);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_KEEP_LONG)
				.withDescription("don't remove finished jobs")
				.isRequired(false)
				.create(OPT_KEEP)
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_UPDATEINTERVAL_LONG)
				.withDescription("minimum update interval (millis)")
				.isRequired(false)
				.hasArg()
				.withArgName("UpdateInterval")
				.create(OPT_UPDATEINTERVAL)
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_MAXRUNNING_LONG)
				.withDescription("maximum number of jobs at a time")
				.isRequired(false)
				.hasArg()
				.withArgName("MaxRunningJobs")
				.create(OPT_MAXRUNNING)
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_MAXREQUESTS_LONG)
				.withDescription("maximum number of jobs submitted at a time")
				.isRequired(false)
				.hasArg()
				.withArgName("MaxNewJobs")
				.create(OPT_MAXREQUESTS)
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_NUMTHREADS_LONG)
				.withDescription("number of concurrent client threads")
				.isRequired(false)
				.hasArg()
				.withArgName("NumberOfThreads")
				.create(OPT_NUMTHREADS)
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_NOCHECKRESOURCES_LONG)
				.withDescription("do not check if required resources are available")
				.isRequired(false)
				.create(OPT_NOCHECKRESOURCES)
		);
		getOptions().addOption(OptionBuilder.withLongOpt("noFetchOutcome")
				.withDescription("Do NOT get stdout/stderr")
				.isRequired(false)
				.create("X")
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SITENAME_LONG)
				.withDescription("Site Name")
				.withArgName("Vsite")
				.hasArg()
				.isRequired(false)
				.create(OPT_SITENAME)
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_WEIGHTS_LONG)
				.withDescription("file containing site weights")
				.withArgName("fileName")
				.hasArg()
				.isRequired(false)
				.create(OPT_WEIGHTS)
		);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SUBMIT_ONLY_LONG)
				.withDescription("only submit jobs, do not wait for completion")
				.isRequired(false)
				.create(OPT_SUBMIT_ONLY)
		);

	}


	@Override
	public String getName() {
		return "batch";
	}

	@Override
	public String getDescription() {
		return "run ucc on a set of files";
	}

	@Override
	public String getSynopsis() {
		return "Runs UCC on all job files in a given directory. In 'follow' mode, " +
		"UCC will wait for new jobs and process them as they arrive.";
	}

	@Override
	public String getArgumentList() {
		return "";
	}
	@Override
	public String getCommandGroup(){
		return "Job execution";
	}

	@Override
	protected void initRegistryClient(){
		super.initRegistryClient();
		//verbose("Creating caching registry.");
		//registry = new CachingRegistryClient(registry);
	}

	@Override
	public void process() {
		//register exit hook
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				doShutdown();
			}
		});
		super.process();

		String inDir=getCommandLine().getOptionValue(OPT_INPUTDIR, properties.getProperty(OPT_INPUTDIR_LONG));
		inputDir=new File(inDir);
		if(!inputDir.exists() || !inputDir.isFile()){
			try{
				inputDir.mkdirs();
				verbose("Created request directory <"+inDir+">");
			}
			catch(Exception ex){
				error("Can't access directory <"+inDir+">.", null);
				endProcessing(1);
			}
		}

		follow=getBooleanOption(OPT_FOLLOW_LONG, OPT_FOLLOW);
		verbose("Follow mode = "+follow);

		keepJobs=getBooleanOption(OPT_KEEP_LONG, OPT_KEEP);
		verbose("Cleaning up done jobs = "+!keepJobs);

		runningJobLimit=getNumericOption(OPT_MAXRUNNING_LONG, OPT_MAXRUNNING, DEFAULT_LIMIT);
		verbose("Limit on number of running jobs = "+runningJobLimit);

		requestLimit=getNumericOption(OPT_MAXREQUESTS_LONG, OPT_MAXREQUESTS, DEFAULT_REQUEST_LIMIT);
		if(requestLimit!=DEFAULT_REQUEST_LIMIT)verbose("Limit on number of new job submissions = "+requestLimit);

		updateInterval=getNumericOption(OPT_UPDATEINTERVAL_LONG, OPT_UPDATEINTERVAL, DEFAULT_UPDATE);
		verbose("Update interval = "+updateInterval+" ms.");

		numThreads=getNumericOption(OPT_NUMTHREADS_LONG, OPT_NUMTHREADS, DEFAULT_THREADS);
		verbose("Number of executor threads = "+numThreads);

		noResourceCheck=getBooleanOption(OPT_NOCHECKRESOURCES_LONG, OPT_NOCHECKRESOURCES);
		verbose("Checking available resources = "+!noResourceCheck);

		noFetchOutcome=getBooleanOption("noFetchOutcome", "X");
		verbose("Getting standard output and standard error = "+!noFetchOutcome);

		submitOnly=getBooleanOption(OPT_SUBMIT_ONLY_LONG, OPT_SUBMIT_ONLY);
		if(submitOnly)verbose("'Submit only' mode = "+submitOnly);

		siteName=getCommandLine().getOptionValue(OPT_SITENAME);
		if(siteName!=null)verbose("Using site = "+siteName);

		String siteWeightFile=getCommandLine().getOptionValue(OPT_WEIGHTS);
		if(siteWeightFile!=null){
			File swf=new File(siteWeightFile);
			if(! (swf.exists() && swf.canRead()) ){
				error("Can't read "+swf.getAbsolutePath(),null);
			}
			else{
				siteSelectionStragegy=new WeightedSelection(swf,this);
				verbose("Using site selection weights from "+swf.getAbsolutePath());
			}

		}
		executor=new ThreadPoolExecutor(numThreads, numThreads,
				500L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		try{
			doBatch();
		}catch(Exception e){
			error("Error executing batch command.", e);
			endProcessing(1);
		}

		doShutdown();
	}

	private void doShutdown(){
		if(isShutdown)return;
		isShutdown=true;
		message("UCC batch mode exiting.");
		while(Runner.getCounter()>0){
			message("... waiting for tasks to finish.");
			try{
				Thread.sleep(2000);
			}catch(InterruptedException ie){}
		}
		executor.shutdown();
		message("Site selection summary:");
		message(printSelectionStatistics(siteSelectionStragegy.getSelectionStatistics()));
	}

	protected void doBatch() throws IOException, InterruptedException{
		requests=new RequestQueue(inputDir.getAbsolutePath(),follow);
		running=new RunningQueue(inputDir.getAbsolutePath()+File.separator+"RUNNING_JOBS");
		running.setLimit(runningJobLimit);
		running.setDelay(updateInterval);
		//initialise to correct value in case batch mode is re-started 
		activeJobs.set(running.length());
		verbose("Starting batch processing...");
		do{
			//submit as many new jobs as allowed and possible
			while(activeJobs.get()<runningJobLimit && activeRequests.get()<requestLimit){
				String nextReq=(String)requests.next();
				if (nextReq!=null){
					verbose("Processing request: "+nextReq);
					numRequests++;
					activeRequests.incrementAndGet();
					createRequest(nextReq);
				}
				else{
					break;
				}
			}

			if(!submitOnly){
				//now process the running jobs 
				String nextRunning=(String)running.next(100,TimeUnit.MILLISECONDS);
				if(nextRunning!=null){
					handleRunningJob(nextRunning);
				}
			}
			else{
				Thread.sleep(100);
			}

		}while(follow || activeRequests.get()>0 || activeJobs.get()>0 || requests.length()>0);

		executor.shutdown();
		verbose("Exiting, "+numRequests+" requests were processed.");
	}

	protected void createRequest(String nextReq){
		try{
			final UCCBuilder b;
			File f=new File(nextReq);
			b=new UCCBuilder(f, registry, configurationProvider);
			b.setMessageWriter(this);
			b.setProperty("source", nextReq);
			String requestName=f.getName();
			//split off extension
			int i=requestName.lastIndexOf('.');
			if(i>0)requestName=requestName.substring(0, i);
			b.setProperty("requestName", requestName);
			b.setProperty("state", "NEW");

			if(siteName!=null) {
				b.setProperty("Site", siteName);
			}
			f.delete();

			executor.execute(new Runnable(){
				public void run(){
					processRequest(b);
				}});
		}catch(Exception e){
			error("",e);
		}
	}


	protected void processRequest(UCCBuilder b){
		try{
			if(isShutdown)return;
			b.setProperty("Output",output.getAbsolutePath());
			b.setProperty("IDLocation",running.getRequestDir().getAbsolutePath());
			Runner r = new Runner(registry, configurationProvider, b, this);
			r.setAsyncMode(true);
			r.setQuietMode(false);
			//TODO r.setCheckResources(!noResourceCheck);
			r.setNoFetchOutCome(noFetchOutcome);
			r.setSubmitOnly(submitOnly);
			r.setSiteSelectionStrategy(siteSelectionStragegy);
			r.setProperties(properties);
			if(siteName!=null){
				b.setProperty("Site", siteName);
				r.setSiteName(siteName);
			}
			r.run();
			if(!submitOnly){
				verbose("Job ID="+r.getBuilder().getProperty("jobIdFile"));
				activeJobs.incrementAndGet();
			}
		}catch(Exception e){
			error("Error processing request <"+b.getProperty("source")+">",e);
			//TODO move to 'failed' location
			endProcessing(1);
		}finally{
			activeRequests.decrementAndGet();
		}
	}

	protected void handleRunningJob(String nextRunning){
		try{
			if(isShutdown)return;
			final File f=new File(nextRunning);
			final UCCBuilder b=new UCCBuilder(f, registry, configurationProvider);
			b.setMessageWriter(this);
			b.setProperty("Output",output.getAbsolutePath());
			b.setProperty("IDLocation",running.getRequestDir().getAbsolutePath());
			b.setProperty("KeepFinishedJob",""+keepJobs);
			b.setProperty("source", nextRunning);
			f.delete();
			executor.execute(new Runnable(){
				public void run(){
					processRunning(b);
				}});
		}
		catch(Exception e){}
	}

	protected void processRunning(UCCBuilder b){
		if(isShutdown)return;
		String req=b.getProperty("source");
		try{
			logger.debug("Processing running job <"+req+">");
			Runner r=new Runner(registry, configurationProvider, b, this);
			r.setAsyncMode(true);
			//TODO r.setCheckResources(!noResourceCheck);
			r.setNoFetchOutCome(noFetchOutcome);
			r.setProperties(properties);
			r.run();
			String state=r.getBuilder().getProperty("state");
			if (Runner.FINISHED.equals(state)){
				activeJobs.decrementAndGet();
			}
			else{
				FileWriter fw=new FileWriter(new File(req));
				b.writeTo(fw);
				fw.close();
			}
		}catch(Exception e){
			error("Error processing running job <"+req+">",e);
		}
	}

	public String printSelectionStatistics(Map<String,AtomicInteger>stats){
		StringBuilder sb=new StringBuilder();
		String newline=System.getProperty("line.separator");
		sb.append(newline);
		sb.append(String.format("  %-52s | %8s", "Site URL", "Jobs run"));
		sb.append(newline);
		sb.append("  ---------------------------------------");
		sb.append(newline);
		for(String site: stats.keySet()){
			sb.append(String.format("  %-52s | %8d", site, stats.get(site).get()));
			sb.append(newline);
		}
		return sb.toString();
	}

}
