package de.fzj.unicore.ucc.workflow;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.ucc.actions.info.ListActionBase;
import eu.unicore.ucc.workflow.WorkflowFactoryLister;
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
		return "[<url> | <url-file>]";
	}

	@Override
	public String getSynopsis(){
		return "Lists info about workflows. " +
				"The workflow address is given directly or is read from <workflow-file>, " +
				"if not given, all workflows are shown.";
	}

	@Override
	public String getDescription(){
		return "lists info on workflows.";
	}
	@Override
	public String getCommandGroup(){
		return "Workflow";
	}


	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt("nofiles")
				.withDescription("Do not list workflow files (in detailed mode)")
				.isRequired(false)
				.create("N")
				);
		getOptions().addOption(OptionBuilder.withLongOpt("nojobs")
				.withDescription("Do not list jobs (in detailed mode)")
				.isRequired(false)
				.create("j")
				);
	}

	@Override
	public void process(){
		super.process();

		listFiles=detailed && !getBooleanOption("nofiles", "N");

		listJobs=detailed && !getBooleanOption("nojobs", "j");

		if(detailed){
			verbose("Listing jobs: "+listJobs);
			verbose("Listing names of files: "+listFiles);
		}
		try{
			run();
		}catch(Exception e){
			error("",e);
			endProcessing(1);
		}
	}

	//read the workflow address
	protected Endpoint initEPR()throws Exception{
		String arg=null;
		Endpoint workflowEPR;

		if(doFilter || getCommandLine().getArgs().length<2)return null;

		arg=getCommandLine().getArgs()[1];

		File wf=new File(arg);
		if(wf.exists())
		{
			workflowEPR = new Endpoint(FileUtils.readFileToString(wf, "UTF-8").trim());
			verbose("Read Workflow address from <"+arg+">");
		}
		else{
			//arg is an Address
			workflowEPR = new Endpoint(arg);
		}
		String url = workflowEPR.getUrl();
		verbose("Checking workflow at "+url);
		return workflowEPR;
	}

	protected WorkflowClient createClient(Endpoint ep) throws Exception {
		return new WorkflowClient(ep, 
				configurationProvider.getClientConfiguration(ep.getUrl()),
				configurationProvider.getRESTAuthN());
	}
	
	protected void run() throws Exception{
		Endpoint singleEPR = initEPR();
		if(singleEPR!=null){
			WorkflowClient wf = createClient(singleEPR);
			if(filterMatch(wf)){
				listOne(wf);
			}
			return;
		}
		WorkflowFactoryLister workflowFactories = new WorkflowFactoryLister(registry, configurationProvider);
		for(WorkflowFactoryClient wf: workflowFactories){
			try{
				verbose("Listing workflows from "+wf.getEndpoint().getUrl());
				EnumerationClient jobs = wf.getWorkflowList();
				jobs.setDefaultTags(tags);
				Iterator<String>urls = jobs.iterator();
				while(urls.hasNext()) {
					String url = urls.next();
					try {
						WorkflowClient wfc = new WorkflowClient(new Endpoint(url), 
								configurationProvider.getClientConfiguration(url),
								configurationProvider.getRESTAuthN());
						if(filterMatch(wfc)){
							listOne(wfc);
						}
					}catch(Exception e) {
						error("Error accessing workflow  "+url, e);						
					}
				}
			}catch(Exception e){
				error("Error accessing workflows at: "+wf.getEndpoint().getUrl(), e);
			}

		}
	}

	protected void listOne(WorkflowClient workflow) throws Exception{
		message(workflow.getEndpoint().getUrl()+getDetails(workflow));;
		printProperties(workflow);
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


	protected void listFiles(WorkflowClient workflow, StringBuilder details){
		try{
			details.append(sep).append("  Files: ");
			BaseServiceClient fileListClient = workflow.getFileList();
			JSONObject props = fileListClient.getProperties();
			@SuppressWarnings("unchecked")
			Iterator<String> iter = (Iterator<String>)props.keys();
			while(iter.hasNext()){
				String wf = iter.next();
				String url = props.getString(wf);
				details.append(sep).append("    ").append(wf).append(" : ").append(url);
			}
		}catch(Exception e){
			String msg = Log.createFaultMessage("Can't list workflow files!", e);
			details.append(sep).append("  ** ERROR: ").append(msg);
		}
	}

	protected void listParameters(JSONObject props, StringBuilder details){
		try{
			for(Map.Entry<String,String>e: JSONUtil.asMap(props.getJSONObject("parameters")).entrySet()){
				details.append(sep).append("  Parameter: ").append(e.getKey()).append("=").append(e.getValue());
			}
		}catch(Exception e){
			String msg = Log.createFaultMessage("Can't process workflow parameters!", e);
			details.append(sep).append("  ** ERROR: ").append(msg);
		}
	}
	
	
}
