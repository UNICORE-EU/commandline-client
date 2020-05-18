package de.fzj.unicore.ucc.workflow;

import java.io.File;
import java.util.Map;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.ucc.actions.info.ListActionBase;
import eu.unicore.ucc.workflow.WorkflowFactoryLister;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowFactoryClient;

/**
 * creates a workflow listing
 * @author schuller
 */
public class WorkflowInfo extends ListActionBase<BaseServiceClient> {

	private boolean listFiles;

	private boolean listJobs;

	private String sep=System.getProperty("line.separator");

	@Override
	public String getName() {
		return "list-workflows";
	}

	@Override
	public String getArgumentList(){
		return "[<workflow-file>]";
	}

	@Override
	public String getSynopsis(){
		return "Lists info about workflows. " +
				"The workflow address is read from <workflow-file>, " +
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
				.withDescription("Do not list global files (in detailed mode)")
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
		WorkflowFactoryLister workflowFactories = new WorkflowFactoryLister(registry, configurationProvider, tags);
		for(WorkflowFactoryClient wf: workflowFactories){
			try{
				verbose("Listing workflows from "+wf.getEndpoint().getUrl());
				JSONArray list = wf.getProperties().getJSONArray("workflows");
				for(String url : JSONUtil.toArray(list)) {
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
	protected String getDetails(BaseServiceClient workflow)throws Exception{
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
		
		if(listFiles){
			listFiles(workflow, details);
		}
		try{
			JSONArray jobs = props.getJSONArray("jobs");
			details.append(sep).append("  Number of jobs: ").append(jobs.length());
			if(listJobs){
				
			}
		}catch(Exception e){
			logger.error("Can't access job list.",e);
		}
		listParameters(props, details);
		return details.toString();
	}


	protected void listFiles(BaseServiceClient workflow, StringBuilder details){
		try{
			// TODO
		}catch(Exception e){
			logger.error("Can't retrieve output files",e);
		}
	}

	protected void listParameters(JSONObject props, StringBuilder details){
		try{
			for(Map.Entry<String,String>e: JSONUtil.asMap(props.getJSONObject("parameters")).entrySet()){
				details.append(sep).append("  Parameter: ").append(e.getKey()).append("=").append(e.getValue());
			}
		}catch(Exception e){
			logger.error("Can't process workflow parameters.",e);
		}
	}
	
	
}
