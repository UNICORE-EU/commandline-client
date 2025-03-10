	package eu.unicore.ucc.workflow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.ucc.actions.info.ListActionBase;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowFactoryClient;

/**
 * creates a workflow listing
 * @author schuller
 */
public class WorkflowInfo extends ListActionBase<WorkflowClient> {

	private boolean listFiles;

	private boolean listJobs;

	private String sep=System.getProperty("line.separator");

	@Override
	public String getName() {
		return "list-workflows";
	}

	@Override
	public String getArgumentList(){
		return "[<url1> ... <urlN>]";
	}

	@Override
	public String getSynopsis(){
		return "Lists info about workflows(s). "
				+ "If no address is given, all workflows are shown.";
	}

	@Override
	public String getDescription(){
		return "lists info on workflows.";
	}
	@Override
	public String getCommandGroup(){
		return CMD_GRP_WORKFLOW;
	}


	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder("N")
				.longOpt("no-files")
				.desc("Do not list workflow files (in detailed mode)")
				.required(false)
				.build());
		getOptions().addOption(Option.builder("j")
				.longOpt("no-jobs")
				.desc("Do not list jobs (in detailed mode)")
				.required(false)
				.build());
		getOptions().addOption(Option.builder("n")
				.longOpt("no-internal")
				.desc("Skip UNICORE/X-internal workflow engine(s)")
				.required(false)
				.build());
	}

	@Override
	public void process() throws Exception {
		super.process();
		listFiles=detailed && !getBooleanOption("nofiles", "N");
		listJobs=detailed && !getBooleanOption("nojobs", "j");
		if(detailed){
			console.verbose("Listing jobs = {}", listJobs);
			console.verbose("Listing names of files = {}", listFiles);
		}
		run();
	}
	
	protected void run() throws Exception{
		if(getCommandLine().getArgs().length>=2) {
			for(int i=1; i<getCommandLine().getArgs().length; i++) {
				listOne(getCommandLine().getArgs()[i]);
			}
		}
		else {
			// list all
			boolean includeInternal = !getBooleanOption("no-internal", "n");
			WorkflowFactoryLister workflowFactories = new WorkflowFactoryLister(registry, configurationProvider, includeInternal);
			for(WorkflowFactoryClient wf: workflowFactories){
				try{
					console.verbose("Listing workflows from {}", wf.getEndpoint().getUrl());
					EnumerationClient jobs = wf.getWorkflowList();
					jobs.setDefaultTags(tags);
					Iterator<String>urls = jobs.iterator();
					while(urls.hasNext()) {
						String url = urls.next();
						try {
							listOne(url);
						}catch(Exception e) {
							console.error(e, "Error accessing workflow {}", url);						
						}
					}
				}catch(Exception e){
					console.error(e, "Error accessing workflows at: {}", wf.getEndpoint().getUrl());
				}
			}
		}
	}

	protected void listOne(String url) throws Exception{
		WorkflowClient wf = new WorkflowClient(new Endpoint(url), 
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
		if(!filterMatch(wf)){
			return;
		}
		console.info("{}{}", url, getDetails(wf));
		printProperties(wf);
	}

	@Override
	protected String getDetails(WorkflowClient workflow)throws Exception{
		if(!detailed)return "";
		StringBuilder details=new StringBuilder();
		JSONObject props = workflow.getProperties();
		String sep=System.getProperty("line.separator");
		details.append(sep);
		String status = props.getString("status");
		details.append("  Status: ").append(status);
		if("FAILED".equals(status)){
			details.append(sep).append("  Error(s): tbd");
		}
		List<String> tags =  JSONUtil.toList(props.getJSONArray("tags"));
		details.append(sep).append("  Tags: ").append(tags);
		listParameters(props, details);
		if(listFiles){
			listFiles(workflow, details);
		}
		if(listJobs){
			try{
				details.append(sep).append("  Jobs: ");
				EnumerationClient jobListClient = workflow.getJobList();
				for(String u: jobListClient) {
					details.append(sep).append("    ").append(u);
				}
			}catch(Exception e){
				String msg = Log.createFaultMessage("Can't access job list.", e);
				details.append(sep).append("  ** ERROR: ").append(msg);
			}
		}
		return details.toString();
	}

	protected void listFiles(WorkflowClient workflow, StringBuilder details) throws Exception {
		details.append(sep).append("  Files: ");
		BaseServiceClient fileListClient = workflow.getFileList();
		JSONObject props = fileListClient.getProperties();
		Iterator<String> iter = props.keys();
		while(iter.hasNext()){
			String wf = iter.next();
			String url = props.getString(wf);
			details.append(sep).append("    ").append(wf).append(" : ").append(url);
		}
	}

	protected void listParameters(JSONObject props, StringBuilder details) {
		for(Map.Entry<String,String>e: JSONUtil.asMap(props.getJSONObject("parameters")).entrySet()){
			details.append(sep).append("  Parameter: ").append(e.getKey()).append("=").append(e.getValue());
		}
	}
}
