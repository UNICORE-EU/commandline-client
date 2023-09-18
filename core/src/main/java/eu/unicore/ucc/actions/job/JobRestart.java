package eu.unicore.ucc.actions.job;

import java.util.List;

import de.fzj.unicore.uas.util.Pair;
import eu.unicore.client.core.JobClient;
import eu.unicore.ucc.util.UCCBuilder;

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

	protected void performCommand(List<Pair<JobClient,UCCBuilder>>jobs){
		jobs.forEach( x -> restart(x.getM1()));
	}

	protected void restart(JobClient job){
		try{
			job.restart();
		}catch(Exception e){
			error("Can't restart job.",e);
			endProcessing(ERROR);
		}
	}

}
