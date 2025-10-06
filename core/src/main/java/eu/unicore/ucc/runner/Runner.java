package eu.unicore.ucc.runner;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.IJobSubmission;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.services.restclient.UserPreferences;
import eu.unicore.ucc.Constants;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.data.Resolve;
import eu.unicore.ucc.actions.job.Batch;
import eu.unicore.ucc.actions.job.GetOutcome;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.helpers.ConsoleLogger;
import eu.unicore.ucc.io.FileDownloader;
import eu.unicore.ucc.io.FileTransferBase.Mode;
import eu.unicore.ucc.io.FileUploader;
import eu.unicore.ucc.io.Location;
import eu.unicore.ucc.util.JSONUtil;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Log;

/**
 * Helper class that runs a job, using the following sequence of actions 
 * <ul>
 * <li>finding a suitable target system, see {@link #findTSS()}</li>
 * <li>import local files</li>
 * <li>start the job</li>
 * <li>wait for completion</li>
 * <li>get output files</li>
 * </ul>
 * The class uses the "State" pattern to model job state. It is configurable in 
 * many ways, for example async/sync job execution, site selection algorithms etc.
 * It is used from the {@link Batch}, {@link Run} and {@link GetOutcome} actions.
 * 
 * @author schuller
 */
public class Runner implements Runnable {

	private static final AtomicInteger counter=new AtomicInteger(0);

	private ConsoleLogger msg;

	private UCCBuilder builder;

	private IJobSubmission tss;

	private JobClient jobClient;

	private Float progress;

	private Status status;

	private final IRegistryClient registry;

	private UCCConfigurationProvider configurationProvider;

	private Broker broker;

	private boolean quietMode = false;

	private boolean asyncMode = false;

	private boolean mustSave = false;

	private boolean needManualJobStart = true;

	//do not get stdout/stderr when job is done
	private boolean noFetchOutcome = false;

	//do not append job ID to file names
	private boolean briefOutputFiles = false;

	// write stdout/err to console
	private boolean outputToConsole = false;

	// do not submit anything
	private boolean dryRun = false;

	private File output;

	private String siteName;

	/**
	 * user's properties
	 */
	private Properties properties;

	private boolean haveOutDir;

	private String preferredProtocol = "BFT";

	private SiteSelectionStrategy selectionStrategy;

	// state names

	public static final String NEW="NEW";
	public static final String STAGEIN="STAGEIN";
	public static final String READY="READY";
	public static final String STARTED="STARTED";
	public static final String STAGEOUT="STAGEOUT";
	public static final String DONE="DONE";
	public static final String FINISHED="FINISHED";
	public static final String EXITING="EXITING";

	public Runner(IRegistryClient registry, UCCConfigurationProvider configurationProvider, UCCBuilder builder){
		this(registry,configurationProvider,builder,UCC.console);
	}

	public Runner(IRegistryClient registry, UCCConfigurationProvider configurationProvider,UCCBuilder builder, ConsoleLogger writer){
		counter.incrementAndGet();
		this.registry=registry;
		this.builder=builder;
		this.msg=writer;
		this.configurationProvider=configurationProvider;
	}

	public void setQuietMode(boolean quietMode){
		this.quietMode = quietMode;
	}

	public void setBroker(Broker broker){
		this.broker=broker;
	}

	public void setAsyncMode(boolean asyncMode){
		this.asyncMode=asyncMode;
	}

	public void setNoFetchOutCome(boolean flag){
		this.noFetchOutcome=flag;
	}

	public void setSiteName(String siteName){
		this.siteName=siteName;
	}

	public void setBriefOutfileNames(boolean flag){
		this.briefOutputFiles=flag;
	}

	public void setDryRun(boolean flag){
		this.dryRun=flag;
	}

	public void setOutputToConsole(boolean toConsole){
		this.outputToConsole = toConsole;
	}

	public void setSiteSelectionStrategy(SiteSelectionStrategy strategy) {
		this.selectionStrategy = strategy;
	}

	public void setSubmissionService(IJobSubmission tss) {
		this.tss = tss;
	}

	public UCCBuilder getBuilder() {
		return builder;
	}

	public JobClient getJob() {
		return jobClient;
	}

	public String getJobID() throws Exception {
		return jobClient!=null ? 
			JSONUtil.extractResourceID(jobClient.getEndpoint().getUrl()) : null;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * get the number of currently executing Runner instances
	 */
	public static int getCounter() {
		return counter.intValue();
	}

	/**
	 * Convenience method that waits until a job has finished
	 * and returns the final status (SUCCESSFUL or FAILED)
	 * @param timeout in milliseconds (null for no timeout)
	 * @return status string
	 */
	public static String waitUntilDone(JobClient job, int timeout) throws Exception{
		job.poll(Status.SUCCESSFUL, timeout);
		return job.getStatus().toString();
	}

	/**
	 * processes the job. Errors during processing are handled differently depending
	 * on the async mode flag.<br/>
	 * In async mode, a JSON file containing the job definition and 
	 * the current state if processing is written to the output directory.<br/> 
	 * In sync mode, simply a runtime exception is thrown.<br/>
	 */
	public void run() {
		try{
			setOutputLocation();
			if(siteName==null)siteName=builder.getSite();
			initPreferredProtocols();
			if(asyncMode)runAsync();
			else runSync();
		}finally{
			counter.decrementAndGet();
		}
	}

	/**
	 * async processing, which progresses the job through a series of states, 
	 * quitting at convenient points to allow high throughput. When quitting the job
	 * is written to a .job file (JSON format, same as the initial .u file), 
	 * with the current state included in the property "state". 
	 */
	private void runAsync(){
		String state = builder.getState();
		State s = getState(state);
		do{
			try{
				mustSave=false;
				s=s.process(this);
				state = s.getName();
				builder.setState(state);
			}catch(Exception e){
				String reason = Log.createFaultMessage("Error occurred processing state <"+s.getName()+">",e);
				throw new RuntimeException(reason, e);
			}
		}while(s.autoProceedToNextState());
		if(!FINISHED.equals(state))writeJobIDFile();
	}

	/**
	 * process the job in one go until it is finished or runs into an error
	 * @throws RuntimeException in case of errors, with the cause initialised
	 * to the root exception
	 */
	private void runSync(){
		String state = builder.getState();
		do{
			State s=getState(state);
			msg.verbose("Job is "+state);
			try{
				s=s.process(this);
				state=s.getName();
				builder.setState(state);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}while(!FINISHED.equals(state) && !EXITING.equals(state) );
	}

	/**
	 * submits a job
	 * @throws RunnerException in case the job could not be submitted
	 */
	private void doSubmit() throws Exception {
		JSONObject submit = builder.getJob();
		if(dryRun){
			listCandidateSites();
			msg.info("Dry-run, NOT submitting, effective JSON:");
			msg.info("{}", submit.toString(2));
			return;
		}
		findTSS();
		msg.verbose("Submission endpoint: {}", tss.getEndpoint().getUrl());

		if(builder.getImports().size()==0){
			submit.put("haveClientStageIn", "false");
			needManualJobStart=false;
		}
		int lifetime=builder.getLifetime();
		if(lifetime>0){
			Calendar c=Calendar.getInstance();
			c.add(Calendar.SECOND, lifetime);
			// submit.put("", ""); // TODO
		}
		// honor group and uid preference from job
		UserPreferences up = ((BaseServiceClient)tss).getUserPreferences();
		String grp = builder.getProperty("Group", null);
		if(grp!=null && up.getGroup()==null) {
			up.setGroup(grp);
		}
		String uid = builder.getProperty("User", null);
		if(uid!=null && up.getUid()==null) {
			up.setUid(uid);
		}
		jobClient = tss.submitJob(submit);
		String url = jobClient.getEndpoint().getUrl();
		builder.setProperty("_ucc_epr", url);
		builder.setProperty("_ucc_type", "job");
		msg.info(url);
		properties.put(Constants.PROP_LAST_RESOURCE_URL, url);
	}

	/**
	 * builds a job client for a running job
	 */
	private void initJobClient(){
		if(jobClient!=null)return;
		try{
			String url = builder.getProperty("_ucc_epr");
			jobClient=new JobClient(new Endpoint(url),
					configurationProvider.getClientConfiguration(url),
					configurationProvider.getRESTAuthN());
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	/**
	 * find a suitable TSS for job submission.
	 * @throws RunnerException if no matching TSS can be found
	 */
	private void findTSS() throws Exception {
		if(tss!=null) {
			return;
		}
		if(broker==null){
			broker = UCC.getBroker("LOCAL");
		}
		Endpoint epr = broker.findTSSAddress(registry, configurationProvider, builder, selectionStrategy);
		tss = new SiteClient(epr, 
				configurationProvider.getClientConfiguration(epr.getUrl()),
				configurationProvider.getRESTAuthN());
	}

	/**
	 * list suitable TSS for job submission.
	 */
	private void listCandidateSites()throws Exception {
		if(tss!=null) {
			msg.info("Submission endpoint: {}", tss.getEndpoint().getUrl());
			return;
		}
		if(broker==null){
			broker = UCC.getBroker("LOCAL");
		}
		Collection<Endpoint> eprs=broker.listCandidates(registry, configurationProvider, builder);
		if(eprs.size()==0){
			msg.verbose("No matching target system available (try 'connect' or check job requirements)");
		}
		for(Endpoint epr: eprs){
			msg.info("Candidate site: {}", epr.getUrl());
		}
	}

	private void setOutputLocation(){
		String outputLoc=builder.getProperty("_ucc_Output", ".");
		try{
			output=new File(outputLoc);
			if(!output.exists())output.mkdirs();
			if(!output.isDirectory())throw new IllegalArgumentException("<"+outputLoc+"> is not a directory.");
		}catch(Exception e){
			msg.error(e, "Problem with <{}>", outputLoc);
		}
	}

	/**
	 * check whether the update interval has passed and the 
	 * job status should be retrieved from remote
	 */
	private boolean shouldUpdate(){
		String updateInterval=builder.getProperty("_ucc_Update interval","1");
		int updateInMillis=Integer.parseInt(updateInterval)*1000;
		if(updateInMillis<=0)return true;
		String lastUpdate=builder.getProperty("_ucc_lastStatusUpdate");
		if(lastUpdate==null){
			lastUpdate=String.valueOf(System.currentTimeMillis());
			builder.setProperty("_ucc_lastStatusUpdate", String.valueOf(lastUpdate));
			mustSave=true;
		}
		long last=Long.parseLong(lastUpdate);
		long now=System.currentTimeMillis();
		if(now>last+updateInMillis){
			builder.setProperty("_ucc_lastStatusUpdate", String.valueOf(now));
			mustSave=true;
			return true;
		}
		return false;
	}

	private void doGetStdOut()throws Exception{
		String stdout = builder.getProperty("Stdout", "stdout");
		FileDownloader e=new FileDownloader(stdout, stdout, Mode.NORMAL, false);
		if(outputToConsole){
			System.out.println("*** Command output: ");
			System.out.println();
			e.setShowProgress(false);
			e.setTargetStream(System.out);
		}
		waitFor(performExportToLocal(e));
		if(outputToConsole){
			System.out.println();
			System.out.println("*** End of command output.");
		}
	}

	private void doGetStdErr()throws Exception{
		String stderr = builder.getProperty("Stderr", "stderr");
		FileDownloader e = new FileDownloader(stderr, stderr, Mode.NORMAL, false);
		if(outputToConsole){
			System.out.println("*** Error output: ");
			System.out.println();
			e.setShowProgress(false);
			e.setTargetStream(System.out);
		}
		waitFor(performExportToLocal(e));
		if(outputToConsole){
			System.out.println();
			System.out.println("*** End of error output.");
		}
	}

	/**
	 * do all job imports
	 */
	private void doImport() throws Exception{
		List<FileUploader>uploads = builder.getImports();
		List<Future<JSONObject>> results = new ArrayList<>();
		for(FileUploader up: uploads){
			results.add(performImportFromLocal(up));
		}
		checkAll(results);
	}

	/**
	 * do all job exports
	 */
	private void doExport()throws Exception{
		List<FileDownloader>downloads = builder.getExports();
		List<Future<JSONObject>> results = new ArrayList<>();
		for(FileDownloader d: downloads){
			results.add(performExportToLocal(d));
		}
		checkAll(results);
	}

	private Future<JSONObject> performImportFromLocal(FileUploader imp) throws Exception{
		imp.setExtraParameterSource(properties);
		if(imp.getChosenProtocol()==null){
			imp.setPreferredProtocol(preferredProtocol);
		}
		imp.setStorageClient(jobClient.getWorkingDirectory());
		return UCC.executor.submit(imp);
	}

	private Future<JSONObject> performExportToLocal(FileDownloader e)throws Exception{
		File out = new File(e.getTo());
		if(!out.isAbsolute()){
			createOutfileDirectory();
			File f=new File(output, e.getTo());
			e.setTo(f.getAbsolutePath());
		}
		e.setExtraParameterSource(properties);
		if(e.getChosenProtocol()==null){
			e.setPreferredProtocol(preferredProtocol);
		}
		Location l = Resolve.resolve(e.getFrom(), registry, configurationProvider);
		StorageClient sms;
		if(!l.isLocal()){
			String url = l.getSmsEpr();
			sms = new StorageClient(new Endpoint(url), 
					configurationProvider.getClientConfiguration(url),
					configurationProvider.getRESTAuthN());
			e.setFrom(l.getName());
		}
		else{
			sms = jobClient.getWorkingDirectory();
		}
		e.setStorageClient(sms);
		return UCC.executor.submit(e);
	}
	
	private JSONObject waitFor(Future<JSONObject>f) throws Exception {
		while(!f.isDone()) {
			Thread.sleep(200);
		}
		return f.get();
	}

	private void checkAll(List<Future<JSONObject>>results) throws Exception {
		for(Future<JSONObject>f: results) {
			JSONObject r = waitFor(f);
			if(!"OK".equals(r.optString("status"))){
				String desc = r.getString("description");
				String err = r.getString("error");
				if(r.getBoolean("failOnError")) {
					throw new Exception(String.format("File transfer %s failed: %s", desc, err));
				}
				else {
					msg.verbose("Ignoring {}: {}", desc, err);
				}
			}
		}
	}

	private void startJob()throws Exception{
		jobClient.start();
		msg.verbose("Job started: {}", jobClient.getEndpoint().getUrl());
	}

	private void writeJobIDFile(){
		if(quietMode || !mustSave)return;
		try	{
			String dump=builder.getProperty("_ucc_jobIdFile",null);
			File dumpFile=null;
			if(dump==null){
				String loc=builder.getProperty("_ucc_IDLocation",output.getAbsolutePath());
				dumpFile=new File(loc,getJobID()+".job");
				dump=dumpFile.getAbsolutePath();
				builder.setProperty("_ucc_jobIdFile",dumpFile.getAbsolutePath());
			}
			else{
				dumpFile=new File(dump);
			}
			try(FileWriter fw=new FileWriter(dumpFile)){
				builder.writeTo(fw);
			}
			msg.info(dumpFile.getAbsolutePath());
		}
		catch(Exception e){
			msg.error(e, "Could not write job ID file.");
		}
	}

	/**
	 * creates a dedicated output directory for this runner (if not
	 * disabled via the "briefOutputFiles" option). 
	 * The name consists of the unique ID, and
	 * (if present) the name of the request file
	 */
	private void createOutfileDirectory(){
		if(!haveOutDir){
			if(!briefOutputFiles){
				StringBuilder sb = new StringBuilder();
				String id = builder.getProperty("_ucc_id");
				if(id!=null)sb.append(id);
				String req = builder.getProperty("_ucc_requestName");
				if(req!=null){
					if(id!=null)sb.append('_');
					sb.append(req);
				}
				output = new File(output,sb.toString());
				output.mkdirs();
			}
			haveOutDir=true;
		}
	}

	private void initPreferredProtocols(){
//		for(ProtocolType.Enum p : builder.getPreferredProtocols()){
//			preferredProtocols.add(p);
//		}
		preferredProtocol = "BFT";
	}

	private void getStatus(boolean printOnlyIfChanged)throws Exception{
		boolean changed=false;
		Status newStatus=jobClient.getStatus();
		if(!newStatus.equals(status)){
			changed=true;
			status=newStatus;
		}
		StringBuilder sb=new StringBuilder();
		sb.append(newStatus);
		if(status.equals(Status.SUCCESSFUL) || status.equals(Status.FAILED)){
			String exit = jobClient.getProperties().optString("exitCode");
			if(exit!=null && exit.length()>0){
				sb.append(", exit code: ");
				sb.append(exit);
			}
			if(status.equals(Status.FAILED)) {
				try{
					sb.append(", ").append(jobClient.getStatusMessage());
				}catch(Exception ex){}
			}
		}
		if(status.equals(Status.RUNNING)){
			String newProgressS = jobClient.getProperties().optString("progress");
			if(newProgressS!=null && newProgressS.length()>0){
				try {
					Float newProgress = Float.valueOf(newProgressS);
					if(!newProgress.equals(progress)){
						progress=newProgress;
						changed=true;
					}
					sb.append(", progress: ");
					sb.append(100*progress);
					sb.append('%');
				}catch(Exception ex) {}
			}
		}
		if(!printOnlyIfChanged)msg.info(sb.toString());
		else if(changed)msg.info(sb.toString());
	}

	// Runner states

	public static abstract class State {

		private final String name;

		public State(String name) {
			this.name = name;
		}

		public final String getName() {
			return name;
		}

		/**
		 * perform actions for this state
		 * @return the next state
		 */
		public State process(Runner r) throws Exception
		{
			return this;
		}

		/** whether to proceed to the next state (in async mode) **/
		public boolean autoProceedToNextState() {
			return true;
		}
	}

	private static final Map<String,State>states = new HashMap<>();

	static{

		/**
		 * in state NEW, the job is submitted to the TSS. 
		 * The next state is STAGEIN. In dryRun mode, the next state
		 * is EXITING
		 */
		states.put(NEW, new State(NEW){
			public State process(Runner r)throws Exception{
				r.doSubmit();
				r.mustSave=true;
				return r.dryRun? getState(EXITING) : getState(STAGEIN);
			}
		});

		/**
		 * in state STAGEIN, the required local files are 
		 * uploaded to the job working directory. 
		 * The next state is READY.
		 */
		states.put(STAGEIN, new State(STAGEIN){
			public State process(Runner r) throws Exception {
				r.initJobClient();
				r.doImport();
				r.mustSave=true;
				if(r.needManualJobStart){
					return Runner.getState(READY);
				}
				else{
					return Runner.getState(STARTED);
				}
			}
		});

		/**
		 * in state READY, the job is started. 
		 * This state may be skipped if there are
		 * no data uploads, and the server supports the autostart feature.
		 * The next state is STARTED.
		 */
		states.put(READY, new State(READY){
			public State process(Runner r) throws Exception {
				r.initJobClient();
				r.startJob();
				r.mustSave=true;
				return Runner.getState(STARTED);
			}
		});

		/**
		 * in state STARTED, it is checked whether the job has finished execution.
		 * the next state is either STARTED (in case the job is still running) or
		 * STAGEOUT if it has finished successfully.
		 * If the job has failed, a RuntimeException is thrown from the process() 
		 * method.
		 */
		states.put(STARTED, new State(STARTED){
			public State process(Runner r) throws Exception {
				r.initJobClient();
				if(r.asyncMode){
					if(!r.shouldUpdate()){
						return getState(STARTED);
					}
					// check if still running
					r.getStatus(true);
					Status status = r.status;
					if(!Status.SUCCESSFUL.equals(status) &&
							!Status.FAILED.equals(status)){
						r.builder.setProperty("_ucc_Update interval", "0");
						return getState(STARTED);
					}
				}
				else{
					if(!Boolean.parseBoolean(r.builder.getProperty("_ucc_DetailedStatusDisplay", "false"))){
						waitUntilDone(r.jobClient,0);
					}
					else{
						String status="";
						do{
							status = waitUntilDone(r.jobClient, 1000);
							r.getStatus(true);
						}while(!status.equals("SUCCESSFUL") && !status.equals("FAILED"));
					}
				}
				r.mustSave = true;
				return getState(STAGEOUT);
			}
			public boolean autoProceedToNextState(){
				return false;
			}
		});

		/**
		 * in state STAGEOUT, the declared export files are retrieved and written to
		 * the local machine. The next state is DONE. 
		 */
		states.put(STAGEOUT, new State(STAGEOUT){
			public State process(Runner r)throws Exception{
				r.initJobClient();
				if(r.getBuilder().isSweepJob()){
					// TODO what would be the best behaviour here?
					r.msg.verbose("Sweep job, not downloading any output.");
				}
				else{
					if(!r.noFetchOutcome){
						r.doGetStdOut();
						r.doGetStdErr();
					}
					r.doExport();
				}
				r.mustSave=true;
				return getState(DONE);
			}
		});

		/**
		 * in state DONE, any cleanup is performed.
		 * The next and final state is FINISHED.
		 */
		states.put(DONE, new State(DONE){
			public State process(Runner r){
				if(!Boolean.parseBoolean(r.builder.getProperty("_ucc_KeepFinishedJob", "false"))){
					try{
						r.initJobClient();
						r.mustSave=true;
						r.jobClient.delete();
						r.msg.verbose("Deleted done job {}", r.jobClient.getEndpoint().getUrl());
					}catch(Exception e){
						r.msg.error(e, "Could not delete job");
					}
				}		
				return getState(FINISHED);
			}
		});

		states.put(FINISHED, new State(FINISHED){
			public boolean autoProceedToNextState(){
				return false;
			}
		});

		states.put(EXITING, new State(EXITING){
			public boolean autoProceedToNextState(){
				return false;
			}
		});
	}

	public static State getState(String state){
		return states.get(state);
	}

}
