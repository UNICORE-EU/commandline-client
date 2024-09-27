package eu.unicore.ucc.actions.job;

import java.util.List;

import org.apache.commons.cli.Option;

import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;

public class GetStatus extends JobOperationBase {

	boolean detailed = false;
	boolean full = false;

	@Override
	public String getName(){
		return "job-status";
	}

	@Override
	public String getSynopsis(){
		return "Gets the status of UNICORE job(s). " +
				super.getSynopsis();
	}

	@Override
	public String getDescription(){
		return "get job status";
	}

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_DETAILED)
				.longOpt(OPT_DETAILED_LONG)
				.desc("More detailed job status")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_ALL)
				.longOpt(OPT_ALL_LONG)
				.desc("Full job status including log")
				.required(false)
				.build());
	}

	@Override
	protected void processAdditionalOptions(){
		full=getBooleanOption(OPT_ALL_LONG, OPT_ALL_LONG);
		verbose("Showing full job details = "+full);
		detailed=getBooleanOption(OPT_DETAILED_LONG, OPT_DETAILED) || full;
		verbose("Showing detailed job status = "+detailed);
		
	}

	@Override
	protected void performCommand(List<Pair<JobClient,UCCBuilder>>jobs){
		allSuccessful = true;
		jobs.forEach( x -> getStatus(x.getM1()));
	}

	protected void getStatus(JobClient job) {
		try{
			Status status=job.getStatus();
			String url=job.getEndpoint().getUrl();
			StringBuilder sb=new StringBuilder();
			sb.append(url).append(" ");
			sb.append(status);
			if(!status.equals(Status.SUCCESSFUL)){
				allSuccessful=false;
			}
			if(status.equals(Status.SUCCESSFUL)){
				Integer exit = job.getExitCode();
				if(exit!=null)sb.append(" exit code: "+exit);
			}
			if(status.equals(Status.RUNNING)){
				Float progress=job.getProgress();
				if(progress!=null)sb.append(" progress: "+(int)(100*progress)+"%");
			}
			lastStatus=sb.toString();
			if(detailed){
				sb.append("\n");
				sb.append(getDetails(job));
			}
			message(sb.toString());
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * gives some more details about the job /apart from
	 * the overall status and exit code.
	 */
	protected String getDetails(JobClient job){
		StringBuilder sb=new StringBuilder();
		String lineBreak = System.getProperty("line.separator");
		if(detailed){
			try	{
				Status status=job.getStatus();
				sb.append(" Status: ").append(status).append(" ").append(job.getStatusMessage());
				sb.append(lineBreak);
				sb.append(" Queue: ").append(job.getQueue());
				
				if(full){
					List<String> log = job.getLog();
					for(String line: log){
						sb.append(lineBreak).append(" Log: ").append(line);
					}
				}
			}catch(Exception ex){
				sb.append(Log.createFaultMessage("ERROR getting job details",ex));
			}
		}
		return sb.toString();
	}
	
	// - unit testing
	public static String lastStatus;
	public static boolean allSuccessful;

}
