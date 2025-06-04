package eu.unicore.ucc.actions.job;

import java.util.List;

import eu.unicore.client.core.JobClient;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Pair;

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
	protected void performCommand(List<Pair<JobClient,UCCBuilder>>jobs){
		jobs.forEach( x -> restart(x.getM1()));
	}

	private void restart(JobClient job){
		try{
			job.restart();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

}
