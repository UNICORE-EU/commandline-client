	package eu.unicore.ucc.workflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.info.ListActionBase;
import eu.unicore.ucc.lookup.SiteFilter;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowClient;

/**
 * creates a workflow listing
 * @author schuller
 */
public class ListWorkflows extends ListActionBase<WorkflowClient> {

	private boolean listFiles;

	private boolean listJobs;

	private boolean includeInternal;

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
				.get());
		getOptions().addOption(Option.builder("j")
				.longOpt("no-jobs")
				.desc("Do not list jobs (in detailed mode)")
				.required(false)
				.get());
		getOptions().addOption(Option.builder("n")
				.longOpt("no-internal")
				.desc("Skip UNICORE/X-internal workflow engine(s)")
				.required(false)
				.get());
	}
	@Override
	protected void setupOptions() {
		super.setupOptions();
		listFiles=detailed && !getBooleanOption("nofiles", "N");
		listJobs=detailed && !getBooleanOption("nojobs", "j");
		if(detailed){
			console.debug("Listing jobs = {}", listJobs);
			console.debug("Listing names of files = {}", listFiles);
		}
		includeInternal = !getBooleanOption("no-internal", "n");
	}

	@Override
	protected Iterable<WorkflowClient> iterator() throws Exception{
		if(getCommandLine().getArgs().length>=2) {
			List<WorkflowClient> workflows = new ArrayList<>();
			getCommandLine().getArgList().listIterator(1).forEachRemaining(
					(url)-> {
							try{
								WorkflowClient wf = new WorkflowClient(new Endpoint(url), 
										configurationProvider.getClientConfiguration(url),
										configurationProvider.getRESTAuthN());
								workflows.add(wf);
							}catch(Exception e) {
								UCC.console.error(e, "Cannot create client for {}", url);
							}
					});
			return workflows;
		}
		else {
			WorkflowLister lister = new WorkflowLister(UCC.executor, registry, configurationProvider, tags);
			lister.setAddressFilter(new SiteFilter(siteName, blacklist));
			lister.setIncludeInternal(includeInternal);
			return () -> lister.iterator(); 
		}
	}

	@Override
	protected String getDetails(WorkflowClient workflow)throws Exception{
		StringBuilder details=new StringBuilder();
		details.append(workflow.getEndpoint().getUrl());
		JSONObject props = workflow.getProperties();
		details.append(_newline);
		String status = props.getString("status");
		details.append("  Status: ").append(status);
		if("FAILED".equals(status)){
			details.append(_newline).append("  Error(s): tbd");
		}
		List<String> tags =  JSONUtil.toList(props.getJSONArray("tags"));
		details.append(_newline).append("  Tags: ").append(tags);
		listParameters(props, details);
		if(listFiles){
			listFiles(workflow, details);
		}
		if(listJobs){
			try{
				details.append(_newline).append("  Jobs: ");
				EnumerationClient jobListClient = workflow.getJobList();
				for(String u: jobListClient) {
					details.append(_newline).append("    ").append(u);
				}
			}catch(Exception e){
				String msg = Log.createFaultMessage("Can't access job list.", e);
				details.append(_newline).append("  ** ERROR: ").append(msg);
			}
		}
		return details.toString();
	}

	private void listFiles(WorkflowClient workflow, StringBuilder details) throws Exception {
		details.append(_newline).append("  Files: ");
		BaseServiceClient fileListClient = workflow.getFileList();
		JSONObject props = fileListClient.getProperties();
		Iterator<String> iter = props.keys();
		while(iter.hasNext()){
			String wf = iter.next();
			String url = props.getString(wf);
			details.append(_newline).append("    ").append(wf).append(" : ").append(url);
		}
	}

	private void listParameters(JSONObject props, StringBuilder details) {
		for(Map.Entry<String,String>e: JSONUtil.asMap(props.getJSONObject("parameters")).entrySet()){
			details.append(_newline).append("  Parameter: ").append(e.getKey()).append("=").append(e.getValue());
		}
	}
}
