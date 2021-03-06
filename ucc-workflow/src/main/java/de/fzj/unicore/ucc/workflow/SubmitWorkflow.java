package de.fzj.unicore.ucc.workflow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.cli.OptionBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.ucc.IServiceInfoProvider;
import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.util.UCCBuilder;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
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
public class SubmitWorkflow extends WorkflowSystemSubmission implements
		IServiceInfoProvider {

	public static final String OPT_UFILE_LONG = "uccInput";
	public static final String OPT_UFILE = "u";

	public static final String OPT_LIFETIME = "t";
	public static final String OPT_LIFETIME_LONG = "lifetime";

	public static final String OPT_WAIT = "w";
	public static final String OPT_WAIT_LONG = "wait";

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
	
	@Override
	public void process() {
		super.process();

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
		if(unmatchedTemplateParameters.size()>0){
			error("ERROR: No value defined for template parameters: "+unmatchedTemplateParameters, null);
			endProcessing(1);
		}

	
		if(localFiles>0){
			try {
				uploadLocalData();
			} catch (Exception e) {
				error("Can't upload local files.", e);
				endProcessing(1);
			}
		}
		
		JSONObject wf = new JSONObject(workflowToBeSubmitted);
		JSONObject inputSpec = wf.optJSONObject("inputs");
		if(inputSpec==null) {
			inputSpec = new JSONObject();
			wf.put("inputs", inputSpec);
		}
		for(String i: inputs.keySet()) {
			inputSpec.put(i, inputs.get(i));
		}

		if(dryRun){
			message("Resulting workflow: ");
			message(wf.toString(2));
			verbose("Dry run, not submitting.");
			return;
		}
		
		WorkflowClient wmc = wsc.submitWorkflow(wf);
		
		String wfURL = wmc.getEndpoint().getUrl();
		verbose("Workflow URL: " + wfURL);

		// output this to allow nicer shell scripting
		message(wfURL);
		properties.put(PROP_LAST_RESOURCE_URL, wfURL);
		
		if (wait) {
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

	protected void uploadLocalData() throws Exception {
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
		
		@SuppressWarnings("unchecked")
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
	}

	
	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(
				OptionBuilder.withLongOpt(OPT_NAME_LONG).withDescription(
						"Workflow Name").withArgName("Name").hasArg()
						.isRequired(false).create(OPT_NAME));

		getOptions().addOption(
				OptionBuilder.withLongOpt(OPT_UFILE_LONG).withDescription(
						"UCC .u file with stage-in definitions").isRequired(
						false).hasArg().create(OPT_UFILE));

		getOptions().addOption(
				OptionBuilder.withLongOpt(OPT_WAIT_LONG).withDescription(
						"wait for workflow completion").isRequired(false)
						.create(OPT_WAIT));

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
		return "Runs a workflow. "
				+ "The workflow definition is read from <workflow-file>."
				+ "A descriptor file will be written that can be used "
				+ "later with other ucc commands.";
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
		// TODO Auto-generated method stub
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
			error("Error accessing REST service at <"+url+">", ex);
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
