package eu.unicore.ucc.actions.job;

import java.util.List;

import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.ucc.util.UCCBuilder;
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

	protected void performCommand(List<Pair<JobClient,UCCBuilder>>jobs){
		for(Pair<JobClient,UCCBuilder> p: jobs){
			restart(p.getM1());
		}
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
