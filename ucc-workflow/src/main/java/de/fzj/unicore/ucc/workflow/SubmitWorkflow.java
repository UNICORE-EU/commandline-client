package de.fzj.unicore.ucc.workflow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.cli.Option;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.ucc.IServiceInfoProvider;
import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.util.UCCBuilder;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.io.FileUploader;
import eu.unicore.ucc.lookup.StorageFactoryLister;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowClient.Status;
import eu.unicore.workflow.WorkflowFactoryClient;

/**
 * submit a workflow to the UNICORE workflow engine
 * 
 * @author schuller
 */
public class SubmitWorkflow extends ActionBase implements
		IServiceInfoProvider {

	public static final String OPT_UFILE_LONG = "uccInput";
	public static final String OPT_UFILE = "u";

	public static final String OPT_WAIT = "w";
	public static final String OPT_WAIT_LONG = "wait";
	
	public static final String OPT_DRYRUN_LONG="dryRun";
	public static final String OPT_DRYRUN="d";
	
	public static final String OPT_STORAGEURL_LONG="storageURL";
	public static final String OPT_STORAGEURL="S";

	protected String siteName;

	protected UCCBuilder builder;

	protected boolean dryRun = false;
	protected WorkflowFactoryClient wsc;

	protected String workflowToBeSubmitted;

	protected WorkflowClient.Status status;

	protected String workflowFileName;

	protected boolean wait = false;

	protected JSONObject templateArguments;

	protected List<String> unmatchedTemplateParameters = new ArrayList<>();

	protected int localFiles = 0;

	protected Map<String,String> inputs = new HashMap<>();
	
	// e.g. "https://host:port/SITE/rest/core/storages/STORAGENAME"
	protected String storageURL;
	
	// base directory in the storage
	protected String baseDir = "/";

	protected String[] tags;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Site Name")
				.required(false)
				.argName("Site")
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_FACTORY)
				.longOpt(OPT_FACTORY_LONG)
				.desc("URL or site name of storage factory to use")
				.argName("StorageFactory")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_STORAGEURL)
				.longOpt(OPT_STORAGEURL_LONG)
				.desc("Storage URL to upload local files to")
				.argName("StorageURL")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_DRYRUN)
				.longOpt(OPT_DRYRUN_LONG)
				.desc("Dry run, do not submit anything")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_TAGS)
				.longOpt(OPT_TAGS_LONG)
				.desc("Tag the job with the given tag(s) (comma-separated)")
				.required(false)
				.hasArgs()
				.valueSeparator(',')
				.build());
		getOptions().addOption(Option.builder(OPT_UFILE)
				.longOpt(OPT_UFILE_LONG)
				.desc("UCC .u file with stage-in definitions")
				.argName("UCCInputFile")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_WAIT)
				.longOpt(OPT_WAIT_LONG)
				.desc("Wait for workflow completion")
				.required(false)
				.build());
	}

	@Override
	public void process(){
		super.process();
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);

		dryRun=getBooleanOption(OPT_DRYRUN_LONG, OPT_DRYRUN);
		verbose("Dry run = "+dryRun);
	
		if (getCommandLine().hasOption(OPT_WAIT)) {
			wait = true;
		}

		if (getCommandLine().getArgs().length < 2) {
			error("Please supply the name of the workflow file,",
					new IllegalArgumentException());
			endProcessing(1);
		} else {
			workflowFileName = getCommandLine().getArgs()[1];
		}	

		tags = getCommandLine().getOptionValues(OPT_TAGS);
		if(tags!=null) {
			verbose("Workflow tags = " + Arrays.deepToString(tags));
		}

		try {
			findSite();
			createBuilder();
			createWorkflowDataStorage();
			if(storageURL!=null) {
				verbose("Using storage at <"+storageURL);
			}
			run();
		} catch (Exception e) {
			error("", e);
			endProcessing(1);
		}

	}

	protected void createBuilder()throws Exception{
		String uFile = getOption(OPT_UFILE_LONG, OPT_UFILE);
		if (uFile != null) {
			verbose("Reading stage-in and parameter definitions from <" + uFile + ">");
			builder = new UCCBuilder(new File(uFile),registry,configurationProvider);
			//side effect: existence of local files will be checked
			localFiles = builder.getImports().size();
			verbose("Will upload <" + localFiles + "> files");
		}
	}
	
	protected void findSite() {
		try {
			List<Endpoint> available = registry.listEntries("WorkflowServices");
			for (Endpoint epr : available) {
				if (siteName != null) {
					String addr = epr.getUrl();
					if (addr.contains(siteName)) {
						wsc = new WorkflowFactoryClient(epr,
								configurationProvider.getClientConfiguration(addr),
								configurationProvider.getRESTAuthN());
						verbose("Using service <" + addr
								+ "> at requested site <" + siteName + ">");
						return;
					}
				}
			}
			int i = 0;
			if (available.size() == 0) {
				message("No workflow service available!");
				endProcessing(1);
			} else {
				// select one
				i = new Random().nextInt(available.size());
				Endpoint epr = available.get(i);
				wsc =  new WorkflowFactoryClient(epr,
						configurationProvider.getClientConfiguration(epr.getUrl()),
						configurationProvider.getRESTAuthN());
			}
			verbose("Selected workflow service at "
					+ wsc.getEndpoint().getUrl());
		} catch (Exception e) {
			error("Can't find a workflow service.", e);
			endProcessing(1);
		}
	}

	protected void createWorkflowDataStorage() throws Exception {
		if(localFiles<1) {
			return;
		}
		if(getCommandLine().hasOption(OPT_STORAGEURL)) {
			storageURL = getOption(OPT_STORAGEURL_LONG, OPT_STORAGEURL);
			if(!storageURL.endsWith("/"))storageURL = storageURL+"/";
			return;
		}
		StorageFactoryClient sfc = null;
		if(getCommandLine().hasOption(OPT_FACTORY)) {
			String url = getCommandLine().getOptionValue(OPT_FACTORY);
			sfc = new StorageFactoryClient(new Endpoint(url),
					configurationProvider.getClientConfiguration(url), 
					configurationProvider.getRESTAuthN());
		}
		else {
			// use first from registry
			StorageFactoryLister sfl = new StorageFactoryLister(
					UCC.executor, registry, configurationProvider);
			sfc = sfl.iterator().next();
		}
		if(sfc==null){
			error("No suitable storage factory available!",null);
			endProcessing(ERROR);
		}
		// create storage now
		verbose("Creating new storage at <"+sfc.getEndpoint().getUrl());
		storageURL = sfc.createStorage().getEndpoint().getUrl();
	}
	
	protected void run() throws Exception {
		loadWorkflowFromFile();

		handleTemplateParameters();
		
		uploadLocalData();
		
		JSONObject wf = new JSONObject(workflowToBeSubmitted);
		JSONObject inputSpec = wf.optJSONObject("inputs");
		if(inputSpec==null) {
			inputSpec = new JSONObject();
			wf.put("inputs", inputSpec);
		}
		for(String i: inputs.keySet()) {
			inputSpec.put(i, inputs.get(i));
		}

		appendTags(wf);
		
		if(dryRun){
			message("Resulting workflow: ");
			message(wf.toString(2));
			verbose("Dry run, not submitting.");
			return;
		}
		
		WorkflowClient wmc = wsc.submitWorkflow(wf);
		String wfURL = wmc.getEndpoint().getUrl();
		verbose("Workflow URL: " + wfURL);
		message(wfURL);
		properties.put(PROP_LAST_RESOURCE_URL, wfURL);
		
		if (wait) {
			waitForFinish(wmc);
		}

	}

	protected void waitForFinish(WorkflowClient wmc) throws Exception {
		verbose("Waiting for workflow to finish...");
		do {
			Thread.sleep(2000);
			Status newStatus = wmc.getStatus();
			if (newStatus == null) {
				error("Can't get workflow status.", null);
				break;
			}
			if (!newStatus.equals(status)) {
				status = newStatus;
				verbose(status.toString());
			}
		} while (Status.RUNNING.equals(status));
	}

	protected void loadWorkflowFromFile() throws Exception {
		try (FileInputStream fis = new FileInputStream(
				new File(workflowFileName).getAbsolutePath()))
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int b = 0;
			while ((b = fis.read()) != -1) {
				bos.write(b);
			}
			workflowToBeSubmitted = bos.toString();
		}
	}

	protected void uploadLocalData() {
		if(localFiles==0)return;
		try {
			if(!baseDir.endsWith("/"))baseDir = baseDir+"/";
			StorageClient sc = new StorageClient(new Endpoint(storageURL),
					configurationProvider.getClientConfiguration(storageURL),
					configurationProvider.getRESTAuthN());

			for(FileUploader fu: builder.getImports()) {
				String wfFile = fu.getTo();
				if(wfFile.startsWith("wf:")) {
					verbose("Uploading <"+fu.getFrom()+"> as workflow file <"+wfFile+"> ...");
					fu.setTo(baseDir+wfFile.substring(3));
					String url = storageURL+"/files"+fu.getTo();
					inputs.put(wfFile, url);
					if(dryRun){
						verbose("Dry run, not uploading.");
						continue;
					}
					fu.perform(sc, this);
				}
			}
		}catch(Exception e) {
			error("Can't upload local files.", e);
			endProcessing(1);
		}
	}
	

	/**
	 * check if we have arguments with metadata in the workflow
	 * and if yes replace any parameters using the spec in 
	 * the .u file / builder 
	 */
	protected void handleTemplateParameters() throws Exception {
		JSONObject wf = new JSONObject(workflowToBeSubmitted);
		templateArguments = wf.optJSONObject("Template parameters");
		if(templateArguments==null)return;
		
		Iterator<String> keys = templateArguments.keys();
		while(keys.hasNext()){
			String name = keys.next();
			JSONObject arg = templateArguments.getJSONObject(name);
			String val = arg.optString("default", null);
			if(builder!=null) {
				val = builder.getProperty(name, val);
			}
			if(val!=null){
				verbose("Template parameter <"+name+">: using value: <"+val+">");
				workflowToBeSubmitted = workflowToBeSubmitted.replace("${"+name+"}", val);
			}
			else {
				unmatchedTemplateParameters.add(name);
			}
		}
		if(unmatchedTemplateParameters.size()>0){
			error("ERROR: No value defined for template parameters: "+unmatchedTemplateParameters, null);
			endProcessing(1);
		}
	}

	protected void appendTags(JSONObject wf) {
		if(tags!=null&&tags.length>0) {
			JSONArray existingTags = wf.optJSONArray("Tags");
			if(existingTags==null)existingTags = wf.optJSONArray("tags");
			if(existingTags==null) {
				existingTags = new JSONArray();
				JSONUtil.putQuietly(wf, "tags", existingTags);
			}
			try {
				List<String> existing = JSONUtil.toList(existingTags);
				for(String t: tags) {
					if(!existing.contains(t)) {
						existing.add(t);
						existingTags.put(t);
					}
				}
			}catch(JSONException je) {}
		}
	}

	@Override
	public String getName() {
		return "workflow-submit";
	}

	@Override
	public String getArgumentList() {
		return "[<workflow-file>]";
	}

	@Override
	public String getSynopsis() {
		return "Submits a workflow to the UNICORE Workflow engine. "
				+ "The workflow definition is read from <workflow-file>.";
	}

	@Override
	public String getDescription() {
		return "submit a workflow";
	}

	@Override
	public String getCommandGroup() {
		return "Workflow";
	}

	@Override
	public String getServiceName() {
		return "Workflow submission";
	}

	@Override
	public String getType() {
		return "WorkflowServices";
	}

	@Override
	public String getServiceDetails(Endpoint epr, UCCConfigurationProvider configurationProvider) {
		String url = epr.getUrl();
		StringBuilder sb = new StringBuilder();
		try{
			IClientConfiguration securityProperties = configurationProvider.getClientConfiguration(url);
			IAuthCallback auth = configurationProvider.getRESTAuthN();
			BaseClient bc = new BaseClient(url, securityProperties, auth);
			JSONObject props = bc.getJSON();
			serverDetails(sb, props.getJSONObject("server"));
			clientDetails(sb, props.getJSONObject("client"));
		}catch(Exception ex) {
			error("Error accessing service at <"+url+">", ex);
		}
		return sb.toString();
	}

	private void clientDetails(StringBuilder sb, JSONObject client) throws JSONException {
		String cr = System.getProperty("line.separator");
		String role = client.getJSONObject("role").getString("selected");
		sb.append("  * authenticated as: '").append(client.getString("dn")).append("' role='").append(role).append("'");
		sb.append(cr);
	}

	private void serverDetails(StringBuilder sb, JSONObject server) throws JSONException {
		String cr = System.getProperty("line.separator");
		sb.append("* server v").append(server.optString("version", "n/a"));
		
		String dn = null;
		try{
			dn = server.getJSONObject("credential").getString("dn");
		}catch(JSONException ex) {
			dn = server.getString("dn");
		}
		sb.append(" ").append(dn).append(cr);
	}
}
