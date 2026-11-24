package eu.unicore.ucc.actions.job;

import java.util.List;

import eu.unicore.client.core.JobClient;

/**
 * job restart
 * 
 * @author schuller
 */
public class JobRestart extends JobOperationBase {

	@Override
	public String getName(){
		return "job-restart";
	}

	@Override
	public String getSynopsis(){
		return "Restart UNICORE job(s). " +
				super.getSynopsis();
	}

	@Override
	public String getDescription(){
		return "restart job(s)";
	}

	@Override
	protected void performCommand(List<JobClient>jobs){
		jobs.forEach( x -> restart(x));
	}

	private void restart(JobClient job){
		try{
			job.restart();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

}
