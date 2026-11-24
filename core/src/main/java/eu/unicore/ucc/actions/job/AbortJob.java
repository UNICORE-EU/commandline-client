package eu.unicore.ucc.actions.job;

import java.util.List;

import eu.unicore.client.core.JobClient;

public class AbortJob extends JobOperationBase {

	@Override
	public String getName(){
		return "job-abort";
	}

	@Override
	public String getSynopsis(){
		return "Aborts UNICORE job(s). " +
				super.getSynopsis();
	}

	@Override
	public String getDescription(){
		return "abort job(s)";
	}

	@Override
	protected void performCommand(List<JobClient>jobs) {
		jobs.forEach( x -> abort(x));
	}

	private void abort(JobClient job) {
		console.verbose("Job id: {}", job.getEndpoint());
		try{
			job.abort();
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
		console.info("Job aborted.");
	}

}
