package eu.unicore.ucc.actions.job;

import java.util.List;

import de.fzj.unicore.uas.util.Pair;
import eu.unicore.client.core.JobClient;
import eu.unicore.ucc.util.UCCBuilder;

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

	protected void performCommand(List<Pair<JobClient,UCCBuilder>>jobs){
		jobs.forEach( x -> abort(x.getM1()));
	}

	protected void abort(JobClient job){
		try{
			verbose("Job id: " +job.getEndpoint().getUrl());
			job.abort();
			message("Job aborted.");
		}catch(Exception e){
			error("Can't abort job.",e);
			endProcessing(ERROR);
		}
	}
	
}
