package eu.unicore.ucc.runner;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

//import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.ucc.Constants;
import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.helpers.DefaultMessageWriter;
import de.fzj.unicore.ucc.util.JSONUtil;
import de.fzj.unicore.ucc.util.UCCBuilder;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.actions.data.Resolve;
import eu.unicore.ucc.actions.job.Batch;
import eu.unicore.ucc.actions.job.GetOutcome;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.io.FileDownloader;
import eu.unicore.ucc.io.FileTransferBase.Mode;
import eu.unicore.ucc.io.FileUploader;
import eu.unicore.ucc.io.Location;
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

	private MessageWriter msg;

	private UCCBuilder builder;

	protected SiteClient tss;

	protected JobClient jobClient;

	protected Float progress;

	protected Status status;

	protected IRegistryClient registry;

	protected UCCConfigurationProvider configurationProvider;

	protected Broker broker=null;

	// don't write any files
	protected boolean quietMode = false;

	protected boolean asyncMode=false;

	protected boolean checkResources=true;

	protected boolean submitOnly=false;

	protected boolean keepJob=true;

	protected boolean mustSave=false;

	protected boolean needManualJobStart=true;

	//do not get stdout/stderr when job is done
	protected boolean noFetchOutcome;

	//do not append job ID to file names
	protected boolean briefOutputFiles;

	//send stdout/err to console
	protected boolean outputToConsole=false;

	//do not submit jobs
	protected boolean dryRun;

	protected File output;

	protected String siteName=null;

	/**
	 * user's properties
	 */
	protected Properties properties;

	private boolean haveOutDir=false;

	protected final List<String> preferredProtocols=new ArrayList<>();

	protected SiteSelectionStrategy selectionStrategy;
	
	// state names

	public static final String NEW="NEW";
	public static final String STAGEIN="STAGEIN";
	public static final String READY="READY";
	public static final String STARTED="STARTED";
	public static final String STAGEOUT="STAGEOUT";
	public static final String DONE="DONE";
	public static final String FINISHED="FINISHED";
	public static final String EXITING="EXITING";

	//error codes
	public static final String ERR_UNKNOWN="Unkown";
	public static final String ERR_INVALID_JOB_DEFINITION="InvalidJobDefinition";
	public static final String ERR_UNMET_REQUIREMENTS="UnmetJobRequirements";
	public static final String ERR_SITE_UNAVAILABLE="SiteUnavailable";
	public static final String ERR_NO_SITE="NoSiteAvailable";
	public static final String ERR_SUBMIT_FAILED="SubmissionFailed";
	public static final String ERR_LOCAL_IMPORT_FAILED="LocalFileImportFailed";
	public static final String ERR_START_FAILED="JobStartFailed";
	public static final String ERR_GET_JOB_STATUS="GettingJobStatusFailed";
	public static final String ERR_JOB_NOT_COMPLETED_SUCCESSFULLY="JobDidNotCompleteSuccessfully";

	public Runner(){
		counter.incrementAndGet();
	}

	public Runner(IRegistryClient registry, UCCConfigurationProvider configurationProvider, UCCBuilder builder){
		this(registry,configurationProvider,builder,new DefaultMessageWriter());
	}

	public Runner(IRegistryClient registry, UCCConfigurationProvider configurationProvider,UCCBuilder builder, MessageWriter writer){
		this();
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

	public void setSubmitOnly(boolean flag){
		this.submitOnly=flag;
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
	
	public void setSiteSelectionStrategy(SiteSelectionStrategy strategy) {
		this.selectionStrategy = strategy;
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
	 * 
	 * In case of an exception,
	 * it is attempted to write the job to a file in the outcome directory, with a 
	 * prefix "FAILED".
	 * 
	 * @throws RuntimeException in case of errors, with the cause initialised
	 * to the root exception
	 */
	protected void runAsync(){
		String state=builder.getProperty("state",NEW);
		State s=getState(state);
		do{
			try{
				mustSave=false;
				s=s.process(this);
				state=s.getName();
				builder.setProperty("state", state);
			}catch(Exception e){
				String reason=Log.createFaultMessage("Error occurred processing state <"+s.getName()+">",e);
				writeFailedJobIDFile(reason);
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
	protected void runSync(){
		String state=builder.getProperty("state",NEW);
		do{
			State s=getState(state);
			msg.verbose("Job is "+state);
			try{
				s=s.process(this);
				state=s.getName();
				builder.setProperty("state", state);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}while(!FINISHED.equals(state) && !EXITING.equals(state) );
	}

	/**
	 * submits a job
	 * @throws RunnerException in case the job could not be submitted
	 */
	protected void doSubmit() throws Exception {

		JSONObject submit = builder.getJSON();
		
		if(dryRun){
			listCandidateSites();
			msg.message("Dry-run, NOT submitting, effective JSON:");
			msg.message(submit.toString(2));
			return;
		}

		findTSS();
		
		try{
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
		}catch(Exception e){
			throw new RunnerException(ERR_INVALID_JOB_DEFINITION,"Could not setup job definition",e);
		}

		try{
			jobClient = tss.submitJob(submit);
		}catch(Exception e){
			throw new RunnerException(ERR_SUBMIT_FAILED,"Could not submit job",e);
		}

		String url = jobClient.getEndpoint().getUrl();
		builder.setProperty("epr", url);
		builder.setProperty("type", "job");
		msg.message(url);
		properties.put(Constants.PROP_LAST_RESOURCE_URL, url);
	}

	/**
	 * builds a job client for a running job
	 */
	protected void initJobClient(){
		if(jobClient!=null)return;
		try{
			String url = builder.getProperty("epr");
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
	protected void findTSS()throws RunnerException {
		if(broker==null){
			broker = UCC.getBroker("LOCAL", msg);
		}
		try{
			Endpoint epr = broker.findTSSAddress(registry, configurationProvider, builder, selectionStrategy);
			tss = new SiteClient(epr, 
					configurationProvider.getClientConfiguration(epr.getUrl()),
					configurationProvider.getRESTAuthN());
		}
		catch(RunnerException re){
			throw re;
		}
		catch(Exception ex){
			String errorReason=Log.createFaultMessage("Error accessing site(s)", ex);
			throw new RunnerException(ERR_NO_SITE, errorReason, ex);
		}
	}

	/**
	 * list suitable TSS for job submission.
	 */
	protected void listCandidateSites()throws RunnerException {
		if(broker==null){
			broker = UCC.getBroker("LOCAL", msg);
		}
		try{
			Collection<Endpoint> eprs=broker.listCandidates(registry, configurationProvider, builder);
			if(eprs.size()==0){
				msg.verbose("No matching target system available (try 'connect' or check job requirements)");
			}
			for(Endpoint epr: eprs){
				msg.message("Candidate site: "+epr.getUrl());
			}
		}
		catch(RunnerException re){
			throw re;
		}
		catch(Exception ex){
			String errorReason=Log.createFaultMessage("Error accessing site(s)", ex);
			throw new RunnerException(ERR_NO_SITE, errorReason, ex);
		}
	}

	protected void setOutputLocation(){
		String outputLoc=builder.getProperty("Output");
		if(outputLoc==null)outputLoc=".";
		try{
			output=new File(outputLoc);
			if(!output.exists())output.mkdirs();
			if(!output.isDirectory())throw new IllegalArgumentException("<"+outputLoc+"> is not a directory.");
		}catch(Exception e){
			msg.error("Problem with <"+outputLoc+">",e);
		}
	}

	public void setOutputToConsole(boolean toConsole){
		this.outputToConsole = toConsole;
	}

	/**
	 * check whether the update interval has passed and the 
	 * job status should be retrieved from remote
	 */
	protected boolean shouldUpdate(){
		String updateInterval=builder.getProperty("Update interval","1");
		int updateInMillis=Integer.parseInt(updateInterval)*1000;
		if(updateInMillis<=0)return true;
		String lastUpdate=builder.getProperty("lastStatusUpdate");
		if(lastUpdate==null){
			lastUpdate=String.valueOf(System.currentTimeMillis());
			builder.setProperty("lastStatusUpdate", String.valueOf(lastUpdate));
			mustSave=true;
		}
		long last=Long.parseLong(lastUpdate);
		long now=System.currentTimeMillis();
		if(now>last+updateInMillis){
			builder.setProperty("lastStatusUpdate", String.valueOf(now));
			mustSave=true;
			return true;
		}
		return false;
	}

	public UCCBuilder getBuilder() {
		return builder;
	}


	public void setBuilder(UCCBuilder builder) {
		this.builder = builder;
	}


	public MessageWriter getMessageWriter() {
		return msg;
	}

	public void setMessageWriter(MessageWriter msg) {
		this.msg = msg;
	}


	protected void doGetStdOut()throws Exception{
		String stdout="stdout";
		try{
			// stdout=jobClient.getProperties().getString("stdout");
		}
		catch(Exception e){}
		FileDownloader e=new FileDownloader(stdout, stdout, Mode.NORMAL, false);
		if(outputToConsole){
			System.out.println("*** Command output: ");
			System.out.println();
			e.setShowProgress(false);
			e.setTargetStream(System.out);
		}
		doExport(e);
		if(outputToConsole){
			System.out.println();
			System.out.println("*** End of command output.");
		}
	}

	protected void doGetStdErr()throws Exception{
		String stderr="stderr";
		try{
			// stderr=jobClient.getProperties().getString("stderr");
		}
		catch(Exception e){}
		FileDownloader e=new FileDownloader(stderr, stderr, Mode.NORMAL, false);
		if(outputToConsole){
			System.out.println("*** Error output: ");
			System.out.println();
			e.setShowProgress(false);
			e.setTargetStream(System.out);
		}
		doExport(e);
		if(outputToConsole){
			System.out.println();
			System.out.println("*** End of error output.");
		}
	}

	protected void doImport()throws RunnerException{
		doImport(builder.getImports());
	}

	/**
	 * do the imports, including a retry using BFT in case of failure
	 */
	protected void doImport(List<FileUploader>fu)throws RunnerException{
		for(FileUploader e: fu){
			try{
				performImportFromLocal(e);
			}catch(RunnerException re){
				//attempt to recover or fail (and abort / destroy job)?
				if(!"BFT".equals(e.getChosenProtocol())){
					//retry with BFT
					msg.message("ERROR performing file import: "+
							Log.createFaultMessage(re.getErrorReason(), re.getCause()));
					msg.message("Re-trying import using BFT protocol");
					// e.setPreferredProtocols(...);
					performImportFromLocal(e);
				}
			}
		}
	}

	protected void doExport()throws Exception{
		doExport(builder.getExports());
	}

	protected void doExport(FileDownloader ...fd)throws Exception{
		doExport(Arrays.asList(fd));
	}

	/**
	 * do exports, including a retry using BFT in case of error
	 */
	protected void doExport(List<FileDownloader>fd)throws Exception{
		for(FileDownloader e: fd){
			try{
				performExportToLocal(e);
			}catch(RunnerException re){
				//attempt to recover or fail (and abort / destroy job)?
				if(!"BFT".equals(e.getChosenProtocol())){
					//retry with BFT
					msg.message("ERROR performing file export: "+
							Log.createFaultMessage(re.getErrorReason(), re.getCause()));
					msg.message("Re-trying import using BFT protocol");
					// e.setPreferredProtocols(...);
					performExportToLocal(e);
				}
			}
		}
	}



	protected void performImportFromLocal(FileUploader imp) throws RunnerException{
		try{
			imp.setExtraParameterSource(properties);
			if(imp.getChosenProtocol()==null){
				//imp.setPreferredProtocols(preferredProtocols);
			}
			imp.perform(jobClient.getWorkingDirectory(), msg);
		}
		catch(Exception ex){
			if(!imp.isFailOnError()){
				String m=Log.createFaultMessage("Could not perform import of local file <"+imp.getFrom()+">, ignoring.", ex);
				msg.verbose(m);
			}
			else {
				throw new RunnerException(ERR_LOCAL_IMPORT_FAILED,"Import of local file <"+imp.getFrom()+"> failed",ex);
			}
		}
	}


	protected void performExportToLocal(FileDownloader e)throws Exception{
		File out=new File(e.getTo());
		if(!out.isAbsolute()){
			createOutfileDirectory();
			File f=new File(output, e.getTo());
			e.setTo(f.getAbsolutePath());
		}
		e.setExtraParameterSource(properties);
		if(e.getChosenProtocol()==null){
			e.setPreferredProtocols(preferredProtocols);
		}
		try{
			//resolve the "from" address
			Location l = Resolve.resolve(e.getFrom(), registry, configurationProvider, msg);
			StorageClient sms=null;
			if(!l.isLocal()){
				String url = l.getSmsEpr();
				sms=new StorageClient(new Endpoint(url), 
						configurationProvider.getClientConfiguration(url),
						configurationProvider.getRESTAuthN());
				e.setFrom(l.getName());
			}
			else{
				sms=jobClient.getWorkingDirectory();
			}
			e.perform(sms, msg);
		}
		catch(Exception ex){
			if(!e.isFailOnError()){
				String m=Log.createFaultMessage("Could not export file <"+e.getFrom()+">", ex);
				msg.verbose(m);
			}
			else{
				throw ex;
			}
		}
	}

	/**
	 * start the job
	 */
	protected void start()throws RunnerException{
		try{
			//jobClient.waitUntilReady(0);
			jobClient.start();
			msg.verbose("Job started: "+jobClient.getEndpoint().getUrl());
		}
		catch(Exception e){
			throw new RunnerException(ERR_START_FAILED,"Could not start job",e);
		}
	}

	protected void writeJobIDFile(){
		if(quietMode)return;
		if(!mustSave)return;
		try	{
			String dump=builder.getProperty("jobIdFile",null);
			File dumpFile=null;
			if(dump==null){
				String loc=builder.getProperty("IDLocation",output.getAbsolutePath());
				dumpFile=new File(loc,getJobID()+".job");
				dump=dumpFile.getAbsolutePath();
				builder.setProperty("jobIdFile",dumpFile.getAbsolutePath());
			}
			else{
				dumpFile=new File(dump);
			}
			FileWriter fw=new FileWriter(dumpFile);
			builder.writeTo(fw);
			fw.close();
			msg.message(dumpFile.getAbsolutePath());
		}
		catch(Exception e){
			msg.error("Could not write job ID file.",e);
		}
	}

	/**
	 * writes a .job file to the output directory with a prefix "FAILED"
	 * @param errorReason - a short description of the error
	 */
	protected void writeFailedJobIDFile(String errorReason){
		if(quietMode)return;
		try{
			File dumpFile=null;
			dumpFile=new File(output.getAbsolutePath(),"FAILED_"+getJobID()+".job");
			FileWriter fw=new FileWriter(dumpFile);
			if(errorReason!=null){
				builder.setProperty("ucc-errorReason", errorReason);
			}
			builder.writeTo(fw);
			fw.close();
			msg.message(dumpFile.getAbsolutePath());
		}
		catch(Exception e){
			msg.error("Could not write failed job to file.",e);
		}
	}

	/**
	 * creates a dedicated output directory for this runner (if not
	 * disabled via the "briefOutputFiles" option). 
	 * The name consists of the unique ID, and
	 * (if present) the name of the request file
	 */
	protected void createOutfileDirectory(){
		if(!haveOutDir){
			if(!briefOutputFiles){
				StringBuilder sb=new StringBuilder();
				String id=builder.getProperty("id");
				if(id!=null)sb.append(id);
				String req=builder.getProperty("requestName");
				if(req!=null){
					if(id!=null)sb.append('_');
					sb.append(req);
				}

				output=new File(output,sb.toString());
				output.mkdirs();
			}
			haveOutDir=true;
		}
	}

	protected void initPreferredProtocols(){
//		for(ProtocolType.Enum p : builder.getPreferredProtocols()){
//			preferredProtocols.add(p);
//		}
		preferredProtocols.add("BFT");
	}

	protected void getStatus(boolean printOnlyIfChanged)throws Exception{
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
			if(exit!=null){
				sb.append(" exit code: ");
				sb.append(exit);
			}
		}
		if(status.equals(Status.RUNNING)){
			String newProgressS = jobClient.getProperties().optString("progress");
			if(newProgressS!=null && !newProgressS.isEmpty()){
				try {
					Float newProgress = Float.valueOf(newProgressS);
					if(!newProgress.equals(progress)){
						progress=newProgress;
						changed=true;
					}
					sb.append(" progress: ");
					sb.append(100*progress);
					sb.append('%');
				}catch(Exception ex) {}
			}
		}

		if(!printOnlyIfChanged)msg.message(sb.toString());
		else if(changed)msg.message(sb.toString());

	}

	/**
	 * writes the current job's properties doc to a file in the output directory, 
	 * named "job_id.properties" where job_id is the unique ID of the job.
	 * The absolute path of this file is echoed to the messageWriter 
	 *  
	 * @return the absolute path of the properties file
	 */
	public String dumpJobProperties(){
		if(quietMode)return null;
		String path=null;
		try{
			String p = jobClient.getProperties().toString(2);
			String id = JSONUtil.extractResourceID(jobClient.getEndpoint().getUrl());
			File dump = new File(output, id+".properties");
			FileWriter fw = new FileWriter(dump);
			fw.append(p);
			fw.close();
			path = dump.getAbsolutePath();
			msg.message(path);
		}catch(Exception e){
			msg.error("Could not get job properties.",e);
		}
		return path;
	}

	public void dumpJobLog(){
		if(jobClient==null){
			msg.verbose("No job log available, because job was not created.");
			return;
		}
		try{
			List<String> p = JSONUtil.asList(jobClient.getProperties().getJSONArray("log"));
			msg.message("Job log: ");
			msg.message(String.valueOf(p));
		}catch(Exception e){
			msg.error("Could not get job log.",e);
		}
	}
	public IRegistryClient getRegistry() {
		return registry;
	}

	public void setRegistry(IRegistryClient registry) {
		this.registry = registry;
	}

	public JobClient getJob() {
		return jobClient;
	}

	public void setJob(JobClient job) {
		this.jobClient = job;
	}

	public String getJobID() throws Exception {
		return jobClient!=null ? 
			JSONUtil.extractResourceID(jobClient.getEndpoint().getUrl()) : null;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public List<String> getPreferredProtocols() {
		return preferredProtocols;
	}

	public Status getStatus() {
		return status;
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
		Status status = Status.UNDEFINED;
		long start=System.currentTimeMillis();
		long elapsed=0;
		while(true){
			if(timeout>0 && elapsed>timeout)break;
			elapsed=System.currentTimeMillis()-start;
			status = job.getStatus();
			if(status.equals(Status.SUCCESSFUL) || status.equals(Status.FAILED)){
				break;
			}
			Thread.sleep(500);
		}
		return status.toString();
	}
	
	/**
	 * Convenience method that waits until a job is READY
	 * and can be started. If the job is already past the READY state,
	 * an exception is thrown.
	 * @param timeout in milliseconds (null for no timeout)
	 * @return status
	 */
	public static String waitUntilReady(JobClient job, int timeout) throws Exception{
		Status status=Status.UNDEFINED;
		String description = "n/a";
		long start=System.currentTimeMillis();
		long elapsed=0;
		while(true){
			if(timeout>0 && elapsed>timeout)break;
			elapsed=System.currentTimeMillis()-start;
			status = job.getStatus();
			description = job.getProperties().getString("statusMessage");
			if(status.equals(Status.READY))break;
			if(status.equals(Status.FAILED)||status.equals(Status.SUCCESSFUL)){
				throw new Exception("Job is already done, status is <"+status.toString()
					+">, error description is <"+description+">");
			}
			Thread.sleep(500);
		}
		return status.toString();
	}
	
	// Runner states

	public interface State {
		/**
		 * perform actions for this state
		 * @return the next state
		 */
		public State process(Runner r)throws Exception;

		/** the name of this state **/
		public String getName();

		/** whether to proceed to the next state (in async mode) **/
		public boolean autoProceedToNextState();
	}

	private static Map<String,State>states=new HashMap<String, State>();

	static{

		/**
		 * in state NEW, the job is submitted to the TSS. 
		 * The next state is STAGEIN. In dryRun mode, the next state
		 * is EXITING
		 */
		states.put(NEW, new State(){
			public State process(Runner r)throws Exception{
				r.doSubmit();
				r.mustSave=true;
				return r.dryRun? getState(EXITING) : getState(STAGEIN);
			}

			public String getName(){
				return NEW;
			}

			public boolean autoProceedToNextState(){
				return true;
			}
		});

		/**
		 * in state STAGEIN, the required local files are 
		 * uploaded to the job working directory. 
		 * The next state is READY.
		 */
		states.put(STAGEIN, new State(){
			public State process(Runner r)throws RunnerException{
				r.initJobClient();
				try{
					r.doImport();
				}catch(RunnerException re){
					r.msg.message("Data import failed, removing the job.");
					try{
						r.jobClient.delete();
					}catch(Exception ex){
						r.msg.error("Could not cleanup job.", ex);
					}
					throw re;
				}
				r.mustSave=true;
				if(r.needManualJobStart){
					return Runner.getState(READY);
				}
				else{
					return Runner.getState(STARTED);
				}
			}

			public String getName(){
				return STAGEIN;
			}

			public boolean autoProceedToNextState(){
				return true;
			}
		});

		/**
		 * in state READY, the job is started. 
		 * This state may be skipped if there are
		 * no data uploads, and the server supports the autostart feature.
		 * The next state is STARTED.
		 */
		states.put(READY, new State(){
			public State process(Runner r)throws RunnerException{
				r.initJobClient();
				r.start();
				r.mustSave=true;
				return Runner.getState(STARTED);
			}

			public String getName(){
				return READY;
			}

			public boolean autoProceedToNextState(){
				return true;
			}
		});

		/**
		 * in state STARTED, it is checked whether the job has finished execution.
		 * the next state is either STARTED (in case the job is still running) or
		 * STAGEOUT if it has finished successfully.
		 * If the job has failed, a RuntimeException is thrown from the process() 
		 * method.
		 */
		states.put(STARTED, new State(){
			public State process(Runner r)throws RunnerException{
				r.initJobClient();
				if(r.asyncMode){
					if(!r.shouldUpdate()){
						return getState(STARTED);
					}
					try{
						//check if still running
						Status status=r.jobClient.getStatus();
						r.msg.verbose("Status for "+r.jobClient.getEndpoint().getUrl()+": "+status);
						if(!Status.SUCCESSFUL.equals(status) &&
								!Status.FAILED.equals(status)){
							r.builder.setProperty("Update interval", "0");
							return getState(STARTED);
						}
					}catch(Exception ex){
						throw new RunnerException(ERR_GET_JOB_STATUS,"Error getting job status",ex);
					}
				}
				else{
					try{
						if(!Boolean.parseBoolean(r.builder.getProperty("DetailedStatusDisplay", "false"))){
							waitUntilDone(r.jobClient,0);
						}
						else{
							String status="";
							do{
								status = waitUntilDone(r.jobClient, 1000);
								r.getStatus(true);
							}while(!status.equals("SUCCESSFUL") && !status.equals("FAILED"));
						}
					}catch(Exception e){
						throw new RunnerException(ERR_JOB_NOT_COMPLETED_SUCCESSFULLY,"Error waiting for job to finish.",e);
					}
				}
				r.mustSave=true;
				return getState(STAGEOUT);
			}
			public String getName(){
				return STARTED;
			}

			public boolean autoProceedToNextState(){
				return false;
			}
		});

		/**
		 * in state STAGEOUT, the declared export files are retrieved and written to
		 * the local machine. The next state is DONE. 
		 */
		states.put(STAGEOUT, new State(){
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
			public String getName(){
				return STAGEOUT;
			}

			public boolean autoProceedToNextState(){
				return true;
			}
		});

		/**
		 * in state DONE, any cleanup is performed.
		 * The next and final state is FINISHED.
		 */
		states.put(DONE, new State(){
			public State process(Runner r){
				if(!Boolean.parseBoolean(r.builder.getProperty("KeepFinishedJob", "false"))){
					try{
						r.initJobClient();
						r.mustSave=true;
						r.jobClient.delete();
						r.msg.verbose("Deleted done job "+r.jobClient.getEndpoint().getUrl());
					}catch(Exception e){
						r.msg.error("Could not delete job",e);
					}
				}		
				return getState(FINISHED);
			}
			public String getName(){
				return DONE;
			}

			public boolean autoProceedToNextState(){
				return true;
			}
		});

		states.put(FINISHED, new State(){
			public State process(Runner r){
				r.msg.message("Job already finished.");
				return getState(FINISHED);
			}
			public String getName(){
				return FINISHED;
			}

			public boolean autoProceedToNextState(){
				return false;
			}
		});

		states.put(EXITING, new State(){
			public State process(Runner r){
				r.msg.message("Dry-run mode, stopping processing.");
				return getState(EXITING);
			}
			public String getName(){
				return EXITING;
			}

			public boolean autoProceedToNextState(){
				return false;
			}
		});
	}

	public static State getState(String state){
		return states.get(state);
	}

}
