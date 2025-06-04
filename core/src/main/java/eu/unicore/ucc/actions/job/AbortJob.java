package eu.unicore.ucc.actions.job;

import java.util.List;

import eu.unicore.client.core.JobClient;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Pair;

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
	protected void performCommand(List<Pair<JobClient,UCCBuilder>>jobs) {
		jobs.forEach( x -> abort(x.getM1()));
	}

	private void abort(JobClient job) {
		console.verbose("Job id: {}", job.getEndpoint().getUrl());
		try{
			job.abort();
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
		console.info("Job aborted.");
	}
	
}
