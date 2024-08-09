package eu.unicore.ucc.actions.job;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.Option;

import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.runner.RandomSelection;
import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.runner.RunningQueue;
import eu.unicore.ucc.runner.SiteSelectionStrategy;
import eu.unicore.ucc.runner.WeightedSelection;
import eu.unicore.ucc.util.UCCBuilder;

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
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_FOLLOW_LONG)
				.longOpt(OPT_FOLLOW)
				.desc("Follow mode: wait for new job files")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_INPUTDIR)
				.longOpt(OPT_INPUTDIR_LONG)
				.desc("Input directory")
				.required(false)
				.argName("InputDir")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_KEEP)
				.longOpt(OPT_KEEP_LONG)
				.desc("Don't remove finished jobs")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_UPDATEINTERVAL)
				.longOpt(OPT_UPDATEINTERVAL_LONG)
				.desc("Minimum update interval (millis)")
				.required(false)
				.argName("UpdateInterval")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_MAXRUNNING)
				.longOpt(OPT_MAXRUNNING_LONG)
				.desc("Maximum number of jobs running at a time")
				.required(false)
				.argName("MaxRunningJobs")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_MAXREQUESTS)
				.longOpt(OPT_MAXREQUESTS_LONG)
				.desc("Maximum number of jobs submitted at a time")
				.required(false)
				.argName("MaxNewJobs")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_NUMTHREADS)
				.longOpt(OPT_NUMTHREADS_LONG)
				.desc("Number of concurrent client threads")
				.required(false)
				.argName("NumberOfThreads")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_NOCHECKRESOURCES)
				.longOpt(OPT_NOCHECKRESOURCES_LONG)
				.desc("Do not check if required resources are available")
				.required(false)
				.build());

		getOptions().addOption(Option.builder(OPT_NOFETCHOUTCOME)
				.longOpt(OPT_NOFETCHOUTCOME_LONG)
				.desc("Do not download stdout/stderr")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Site Name")
				.required(false)
				.argName("Site")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_WEIGHTS)
				.longOpt(OPT_WEIGHTS_LONG)
				.desc("File containing site weights")
				.required(false)
				.argName("fileName")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_SUBMIT_ONLY)
				.longOpt(OPT_SUBMIT_ONLY_LONG)
				.desc("Only submit jobs, do not wait for completion")
				.required(false)
				.build());
	}


	@Override
	public String getName() {
		return "batch";
	}

	@Override
	public String getDescription() {
		return "run UCC on a set of files";
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
		return CMD_GRP_JOBS;
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

		noFetchOutcome=getBooleanOption(OPT_NOFETCHOUTCOME_LONG, OPT_NOFETCHOUTCOME);
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
				siteSelectionStragegy = new WeightedSelection(swf);
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
			Runner r = new Runner(registry, configurationProvider, b);
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
			b.setProperty("Output",output.getAbsolutePath());
			b.setProperty("IDLocation",running.getRequestDir().getAbsolutePath());
			b.setProperty("KeepFinishedJob", String.valueOf(keepJobs));
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
			Runner r = new Runner(registry, configurationProvider, b);
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
