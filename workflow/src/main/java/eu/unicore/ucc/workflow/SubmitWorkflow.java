package eu.unicore.ucc.workflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.lookup.SiteNameFilter;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.ucc.IServiceInfoProvider;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.io.FileUploader;
import eu.unicore.ucc.lookup.StorageFactoryLister;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowClient.Status;
import eu.unicore.workflow.WorkflowFactoryClient;

/**
 * submit a workflow to the UNICORE workflow engine
 * 
 * @author schuller
 */
public class SubmitWorkflow extends ActionBase implements IServiceInfoProvider {

	private static final String OPT_UFILE_LONG = "ucc-input";
	private static final String OPT_UFILE = "u";

	public static final String OPT_WAIT = "w";
	public static final String OPT_WAIT_LONG = "wait";

	public static final String OPT_STORAGEURL_LONG="storage-url";
	public static final String OPT_STORAGEURL="S";

	private String submissionSite;

	private UCCBuilder builder;

	private boolean dryRun = false;

	private WorkflowFactoryClient wsc;

	private JSONObject workflow;

	private WorkflowClient.Status status;

	private String workflowFileName;

	private boolean wait = false;

	private int localFiles = 0;

	private final Map<String,String> inputs = new HashMap<>();

	private String storageURL;

	private String baseDir = "/";

	private String[] tags;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Site name")
				.required(false)
				.argName("Site")
				.hasArg()
				.get());
		getOptions().addOption(Option.builder(OPT_FACTORY)
				.longOpt(OPT_FACTORY_LONG)
				.desc("URL or site name of storage factory to use")
				.argName("StorageFactory")
				.hasArg()
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_STORAGEURL)
				.longOpt(OPT_STORAGEURL_LONG)
				.desc("Storage URL to upload local files to")
				.argName("StorageURL")
				.hasArg()
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_DRYRUN)
				.longOpt(OPT_DRYRUN_LONG)
				.desc("Dry run, do not submit anything")
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_TAGS)
				.longOpt(OPT_TAGS_LONG)
				.desc("Tag the workflow with the given tag(s) (comma-separated)")
				.required(false)
				.hasArgs()
				.valueSeparator(',')
				.get());
		getOptions().addOption(Option.builder(OPT_UFILE)
				.longOpt(OPT_UFILE_LONG)
				.desc("UCC .u file with stage-in definitions")
				.argName("UCCInputFile")
				.hasArg()
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_WAIT)
				.longOpt(OPT_WAIT_LONG)
				.desc("Wait for workflow completion")
				.required(false)
				.get());
	}

	@Override
	public void process() throws Exception {
		super.process();
		submissionSite = getCommandLine().getOptionValue(OPT_SITENAME);
		dryRun = getBooleanOption(OPT_DRYRUN_LONG, OPT_DRYRUN);
		console.debug("Dry run = {}", dryRun);
		wait =  getCommandLine().hasOption(OPT_WAIT);
		if (getCommandLine().getArgs().length < 2) {
			throw new IllegalArgumentException("Please supply the name of the workflow file.");
		} else {
			workflowFileName = getCommandLine().getArgs()[1];
		}
		tags = getCommandLine().getOptionValues(OPT_TAGS);
		if(tags!=null) {
			console.debug("Workflow tags = {}", Arrays.deepToString(tags));
		}
		findSite();
		createBuilder();
		createWorkflowDataStorage();
		if(storageURL!=null) {
			console.verbose("Using storage at <{}>", storageURL);
		}
		run();
	}

	private void createBuilder()throws Exception{
		String uFile = getOption(OPT_UFILE_LONG, OPT_UFILE);
		if (uFile != null) {
			console.debug("Reading stage-in and parameter definitions from <{}>", uFile);
			builder = new UCCBuilder(new File(uFile),registry,configurationProvider);
			//side effect: existence of local files will be checked
			localFiles = builder.getImports().size();
			console.verbose("Will upload <{}> files", localFiles);
		}
	}

	private void findSite() throws Exception {
		WorkflowFactoryLister workflowFactories = new WorkflowFactoryLister(registry,
				configurationProvider, true);
		if(submissionSite!=null) {
			workflowFactories.setAddressFilter(new SiteNameFilter(submissionSite));
		}
		wsc = workflowFactories.iterator().next();
		if(wsc!=null) {
			console.verbose("Selected workflow service at {}", wsc.getEndpoint().getUrl());
		}
		else {
			throw new Exception("No workflow submission endpoint found!");
		}
	}

	private void createWorkflowDataStorage() throws Exception {
		if(localFiles<1) {
			return;
		}
		if(getCommandLine().hasOption(OPT_STORAGEURL)) {
			storageURL = getOption(OPT_STORAGEURL_LONG, OPT_STORAGEURL);
			if(storageURL.endsWith("/")) {
				storageURL = storageURL.substring(0, storageURL.length()-1);
			}
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
			throw new Exception("No suitable storage factory available!");
		}
		console.verbose("Creating new storage at <{}>", sfc.getEndpoint().getUrl());
		storageURL = sfc.createStorage().getEndpoint().getUrl();
	}
	private void run() throws Exception {
		loadWorkflow();
		uploadLocalData();
		JSONObject inputSpec = workflow.optJSONObject("inputs");
		if(inputSpec==null) {
			inputSpec = new JSONObject();
			workflow.put("inputs", inputSpec);
		}
		for(String i: inputs.keySet()) {
			inputSpec.put(i, inputs.get(i));
		}
		appendTags(workflow);
		if(dryRun){
			console.info("Resulting workflow: ");
			console.info("{}", workflow.toString(2));
			console.info("Dry run, not submitting.");
			return;
		}
		WorkflowClient wmc = wsc.submitWorkflow(workflow);
		String wfURL = wmc.getEndpoint().getUrl();
		lastAddress = wfURL;
		console.info("{}", wfURL);
		properties.put(PROP_LAST_RESOURCE_URL, wfURL);
		if (wait) {
			waitForFinish(wmc);
		}
	}

	private void waitForFinish(WorkflowClient wmc) throws Exception {
		console.verbose("Waiting for workflow to finish...");
		do {
			Thread.sleep(2000);
			Status newStatus = wmc.getStatus();
			if (newStatus == null) {
				console.error(null, "Can't get workflow status.");
				break;
			}
			if (!newStatus.equals(status)) {
				status = newStatus;
				console.verbose("{}", status);
			}
		} while (Status.RUNNING.equals(status));
	}

	private void uploadLocalData() throws Exception {
		if(localFiles==0)return;
		if(!baseDir.endsWith("/"))baseDir = baseDir+"/";
		StorageClient sc = new StorageClient(new Endpoint(storageURL),
				configurationProvider.getClientConfiguration(storageURL),
				configurationProvider.getRESTAuthN());
		for(FileUploader fu: builder.getImports()) {
			String wfFile = fu.getTo();
			if(wfFile.startsWith("wf:")) {
				console.verbose("Uploading <{}> as workflow file <{}> ...", fu.getFrom(), wfFile);
				fu.setTo(StorageClient.normalize(baseDir+wfFile.substring(3)));
				String url = storageURL+"/files"+fu.getTo();
				inputs.put(wfFile, url);
				if(dryRun){
					console.verbose("Dry run, not uploading.");
					continue;
				}
				fu.perform(sc);
			}
		}
	}

	/**
	 * Loads workflow from file. 
	 * Checks if there are arguments with metadata in the workflow
	 * and if yes replaces any parameters using the spec in 
	 * the .u file / builder 
	 */
	private void loadWorkflow() throws Exception {
		String wf = FileUtils.readFileToString(new File(workflowFileName), "UTF-8");
		workflow = new JSONObject(wf);
		JSONObject templateArguments = workflow.optJSONObject("Template parameters");
		if(templateArguments==null)return;

		List<String> unmatchedTemplateParameters = new ArrayList<>();
		Iterator<String> keys = templateArguments.keys();
		while(keys.hasNext()){
			String name = keys.next();
			JSONObject arg = templateArguments.getJSONObject(name);
			String val = arg.optString("default", null);
			if(builder!=null) {
				val = builder.getProperty(name, val);
			}
			if(val!=null){
				console.verbose("Template parameter <{}>: using value: <{}>", name, val);
				wf = wf.replace("${"+name+"}", val);
			}
			else {
				unmatchedTemplateParameters.add(name);
			}
		}
		if(unmatchedTemplateParameters.size()>0){
			throw new Exception("ERROR: No value defined for template parameters: "+unmatchedTemplateParameters);
		}
		workflow = new JSONObject(wf);
	}

	private void appendTags(JSONObject wf) {
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
		return "Submits a workflow to a UNICORE Workflow engine. "
				+ "The workflow definition is read from <workflow-file>.";
	}

	@Override
	public String getDescription() {
		return "submit a workflow";
	}

	@Override
	public String getCommandGroup() {
		return CMD_GRP_WORKFLOW;
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
			console.error(ex, "Error accessing service at <{}>", url);
		}
		return sb.toString();
	}

	private void clientDetails(StringBuilder sb, JSONObject client) throws JSONException {
		String role = client.getJSONObject("role").getString("selected");
		sb.append("  * authenticated as: '").append(client.getString("dn")).append("' role='").append(role).append("'");
		sb.append(_newline);
	}

	private void serverDetails(StringBuilder sb, JSONObject server) throws JSONException {
		sb.append("* server v").append(server.optString("version", "n/a"));
		String dn = server.getJSONObject("credential").getString("dn");
		sb.append(" ").append(dn).append(_newline);
	}

	static String lastAddress;
}
