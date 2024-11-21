package eu.unicore.ucc.actions.job;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.Option;

import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;

public class JobStatus extends JobOperationBase {

	boolean detailed = false;
	boolean full = false;
	Status waitFor = null;
	int timeout = -1;

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
		getOptions().addOption(Option.builder(OPT_WAIT)
				.longOpt(OPT_WAIT_LONG)
				.desc("Wait for the given job status ("+waitableJobStatuses+")")
				.hasArg(true)
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_TIMEOUT)
				.longOpt(OPT_TIMEOUT_LONG)
				.desc("Timeout for job status polling")
				.hasArg(true)
				.required(false)
				.build());
	}
	
	@Override
	public Collection<String> getAllowedOptionValues(String option) {
		if(OPT_WAIT.equals(option)) {
			return waitableJobStatuses;
		}
		return null;
	}

	@Override
	protected void processAdditionalOptions(){
		full = getBooleanOption(OPT_ALL_LONG, OPT_ALL);
		verbose("Showing full job details = "+full);
		detailed = getBooleanOption(OPT_DETAILED_LONG, OPT_DETAILED) || full;
		verbose("Showing detailed job status = "+detailed);
		String waitForSpec = getOption(OPT_WAIT_LONG, OPT_WAIT);
		if(waitForSpec!=null) {
			try{
				waitFor = Status.valueOf(waitForSpec);
				if(waitFor==Status.FAILED || waitFor==Status.UNDEFINED) {
					throw new Exception();
				}
			}catch(Exception ex) {
				throw new IllegalArgumentException("'--wait-for' accepts one of: "+Arrays.asList(waitableJobStatuses));
			}
			String timeoutSpec = getCommandLine().getOptionValue(OPT_TIMEOUT);
			if(timeoutSpec!=null) {
				timeout = (int)UnitParser.getTimeParser(0).getDoubleValue(timeoutSpec);
				verbose("Status polling (--wait-for) timeout = "+timeout+" sec.");
			}
			verbose("Waiting for job to be "+waitFor+" ...");
		}
	}

	@Override
	protected void performCommand(List<Pair<JobClient,UCCBuilder>>jobs){
		allSuccessful = true;
		jobs.forEach( x -> {
			JobClient j = x.getM1();
			if(waitFor!=null) {
				try{
					j.poll(waitFor, timeout);
					getStatus(j);
				}catch(TimeoutException te) {
					verbose("Timeout polling "+j.getEndpoint().getUrl());
				}catch(Exception ex) {
					verbose(Log.createFaultMessage("Error polling "+j.getEndpoint().getUrl(), ex));
				}
			}
			else {
				getStatus(j);
			}
		});
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
		StringBuilder sb = new StringBuilder();
		String lineBreak = System.getProperty("line.separator");
		try	{
			if(job.getStatus().equals(Status.FAILED)){
				sb.append(" Error message: ").append(job.getStatusMessage());
				sb.append(lineBreak);
			}
			sb.append(" Working directory: ").append(job.getWorkingDirectory().getEndpoint().getUrl());
			sb.append(lineBreak);
			String t = job.getProperties().optString("jobType","N/A");
			sb.append(" Job type: ").append(t);
			if(!"ON_LOGIN_NODE".equals(t)) {
				sb.append(", queue: '").append(job.getQueue()).append("'");
			}
			if(full){
				for(String line: job.getLog()){
					sb.append(lineBreak).append(" Log: ").append(line);
				}
			}
		}catch(Exception ex){
			sb.append(Log.createFaultMessage("ERROR getting job details",ex));
		}
		return sb.toString();
	}
	
	// - unit testing
	public static String lastStatus;
	public static boolean allSuccessful;

}
