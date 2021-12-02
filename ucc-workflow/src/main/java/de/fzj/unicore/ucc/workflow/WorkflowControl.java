package de.fzj.unicore.ucc.workflow;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import de.fzj.unicore.ucc.UCC;
import eu.unicore.client.Endpoint;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.workflow.WorkflowClient;

/**
 * workflow control: abort, resume (with parameters)
 * 
 * @author schuller
 */
public class WorkflowControl extends ActionBase {
	
	private String workflowURL;
	
	private WorkflowClient workflow;
	
	private String cmd;
	
	final Map<String,String>parameters=new HashMap<>();
	
	@Override
	public String getName() {
		return "workflow-control";
	}
	
	@Override
	public String getArgumentList(){
		return "abort <workflow-address> | resume <workflow-address> [key1=value1 key2=value2 ...]";
	}
	
	@Override
	public String getSynopsis(){
		return "Allows to abort or resume (if held) a workflow ";
	}

	@Override
	public String getDescription(){
		return "offers workflow control functions";
	}

	
	
	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
	}

	@Override
	public void process(){
		super.process();
		try{
			run();
		}catch(Exception e){
			error("",e);
			endProcessing(1);
		}
	}

	//read the workflow address and the command to perform
	protected void getBasicCmdlineParams()throws Exception{
		cmd=getCommandLine().getArgs()[1];
		String arg=null;
		if(getCommandLine().getArgs().length>2)arg=getCommandLine().getArgs()[2];
		
		if(arg==null){
			try{
				arg=new BufferedReader(new InputStreamReader(System.in)).readLine();
			}catch(Exception e){
				error("Can't read workflow address from stdin.",e);
				endProcessing(1);
			}
		}
		
		workflowURL = arg;
		
		verbose("Checking workflow at "+workflowURL);
		workflow=new WorkflowClient(new Endpoint(workflowURL),
				configurationProvider.getClientConfiguration(workflowURL), 
				configurationProvider.getRESTAuthN());
		properties.put(PROP_LAST_RESOURCE_URL, workflowURL);
	}
	
	protected void run() throws Exception{
		getBasicCmdlineParams();
		readExtraParameters();
		if("abort".equalsIgnoreCase(cmd))doAbort();
		if("resume".equalsIgnoreCase(cmd))doResume();
	}
	
	protected void readExtraParameters(){
		parameters.clear();
		int length=getCommandLine().getArgs().length;
		if(length<3)return;
		for(int i=3; i<length; i++){
			String p=getCommandLine().getArgs()[i];
			String[]split=p.split("=");
			String key=split[0];
			String value=split[1];
			verbose("Have parameter: "+key+"="+value);
			parameters.put(key, value);
		}
		//unit testing use
		lastParams=parameters;
	}
	
	protected void doAbort()throws Exception{
		verbose("Aborting workflow "+workflowURL);
		if(!UCC.unitTesting)workflow.abort();
	}
	
	protected void doResume()throws Exception{
		verbose("Resuming workflow "+workflowURL);
		if(!UCC.unitTesting)workflow.resume(parameters);
	}
	
	@Override
	public String getCommandGroup(){
		return "Workflow";
	}
	
	static Map<String,String>lastParams;
	
}
