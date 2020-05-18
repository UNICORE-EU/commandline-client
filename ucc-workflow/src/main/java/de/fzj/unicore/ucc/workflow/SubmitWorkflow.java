package de.fzj.unicore.ucc.workflow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.OptionBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.ucc.IServiceInfoProvider;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.util.UCCBuilder;
import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
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

	public static final String OPT_NAME_LONG = "workflowName";
	public static final String OPT_NAME = "N";

	public static final String OPT_UFILE_LONG = "uccInput";
	public static final String OPT_UFILE = "u";

	public static final String OPT_LIFETIME = "t";
	public static final String OPT_LIFETIME_LONG = "lifetime";

	public static final String OPT_WAIT = "w";
	public static final String OPT_WAIT_LONG = "wait";

	protected String workflowName;

	protected WorkflowFactoryClient wsc;

	protected String workflowToBeSubmitted;

	protected WorkflowClient.Status status;

	protected String workflowFileName;

	protected boolean wait = false;

	protected JSONObject templateArguments;

	protected int unmatchedTemplateParameters;

	@Override
	public void process() {
		super.process();

		workflowName = getCommandLine().getOptionValue(OPT_NAME);
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
			//createWorkflowDataStorage();
			run();
		} catch (Exception e) {
			error("", e);
			endProcessing(1);
		}

	}

	protected void createBuilder()throws Exception{
		String uFile = getOption(OPT_UFILE_LONG, OPT_UFILE);
		if (uFile != null) {
			verbose("Reading stage-in definitions from <" + uFile + ">");
			builder = new UCCBuilder(new File(uFile),registry,configurationProvider);
			//side effect: existence of local files will be checked
			int n=builder.getImports().size();
			verbose("Will upload <" + n + "> files");
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

	protected void run() throws Exception {
		loadWorkflowFromFile();

		String uFile = getOption(OPT_UFILE_LONG, OPT_UFILE);
		if (uFile != null) {
			verbose("Reading stage-in and parameter definitions from <" + uFile + ">");
			builder = new UCCBuilder(new File(uFile),registry,configurationProvider);
			//side effect: existence of local files will be checked
			int n = builder.getImports().size();
			verbose("Will upload <" + n + "> files");
		}
		
		handleTemplateParameters();
		if(unmatchedTemplateParameters>0){
			error("Warning: for some template parameters, no value could be found!", null);
		}

		if(dryRun){
			verbose("Dry run, not submitting.");
			return;
		}
		
	
		if(builder!=null){
//			try {
//				uploadLocalData(builder, id);
//			} catch (IOException e) {
//				error("Can't upload local files.", e);
//				endProcessing(1);
//			}
		}
		
		JSONObject wf = new JSONObject(workflowToBeSubmitted);
		
		WorkflowClient wmc = wsc.submitWorkflow(wf);
		
		//workflowToBeSubmitted = resolveU6URLs(workflowToBeSubmitted);
		String wfURL = wmc.getEndpoint().getUrl();
		verbose("Workflow URL: " + wfURL);
		
		writeIDFile(wfURL);

		// output this to allow nicer shell scripting
		message(wfURL);

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
		FileInputStream fis = new FileInputStream(new File(workflowFileName)
				.getAbsolutePath());
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int b = 0;
			while ((b = fis.read()) != -1) {
				bos.write(b);
			}
			workflowToBeSubmitted = bos.toString();
		} finally {
			fis.close();
		}
	}

	/**
	 * check if we have arguments with metadata in the workflow
	 * and if yes replace any parameters using the spec in 
	 * the .u file / builder 
	 */
	protected void handleTemplateParameters() throws Exception {
		JSONObject wf = new JSONObject(workflowToBeSubmitted);
		templateArguments = wf.optJSONObject("templateParameters");
		if(templateArguments==null)return;
		
		unmatchedTemplateParameters = templateArguments.length();
		@SuppressWarnings("unchecked")
		Iterator<String> keys = templateArguments.keys();
		while(keys.hasNext()){
			String name = keys.next();
			JSONObject arg = templateArguments.getJSONObject(name);
			String defaultValue = arg.optString("default", null);
			String val = builder.getProperty(name, defaultValue);
			if(val!=null){
				verbose("Template parameter <"+arg+">: using value: <"+val+">");
				workflowToBeSubmitted = workflowToBeSubmitted.replace("${"+name+"}", val);
				unmatchedTemplateParameters--;
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

	protected void writeIDFile(String url) {
		try {
			String id = url.substring(url.lastIndexOf('/')+1);
			File dump = new File(output, id + ".workflow-address");
			FileWriter fw = new FileWriter(dump);
			fw.write(url + "\n");
			fw.close();
			verbose("Wrote workflow address to " + dump.getAbsolutePath());
		} catch (Exception e) {
			error("Could not write workflow ID file.", e);
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
