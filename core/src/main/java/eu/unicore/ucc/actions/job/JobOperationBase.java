package eu.unicore.ucc.actions.job;

import java.util.ArrayList;
import java.util.List;

import eu.unicore.client.core.JobClient;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.util.UCCBuilder;

/**
 * base class for job operations
 * 
 * @author schuller
 */
public abstract class JobOperationBase extends ActionBase {

	/**
	 * get job client and job info for the given job URL
	 *
	 * @param jobURL
	 * @return pair containing {@link JobClient} and {@link UCCBuilder}
	 */
	protected JobClient createJobClient(String jobURL) throws Exception {
		return new JobClient(jobURL,
					configurationProvider.getClientConfiguration(jobURL),
					configurationProvider.getRESTAuthN());
	}

	/**
	 * create a {@link UCCBuilder} instance
	 * @param url- job URL
	 */
	protected UCCBuilder createBuilder(JobClient job) throws Exception {
		UCCBuilder builder = new UCCBuilder(registry, configurationProvider);
		builder.setCheckLocalFiles(false);
		builder.setProperty("_ucc_epr", job.getEndpoint());
		console.debug("Accessing job at <{}>", job.getEndpoint());
		return builder;
	}

	@Override
	public void process() throws Exception {
		super.process();
		processAdditionalOptions();
		assertArgsLength(2, "At least one job is required.");
		List<String> args = new ArrayList<>();
		for(int i=1; i<getCommandLine().getArgs().length;i++){
			args.add(getCommandLine().getArgs()[i]);
		}
		List<JobClient>jobClients = new ArrayList<>();
		for(String arg: args){
			jobClients.add(createJobClient(arg));
		}
		performCommand(jobClients);
	}

	protected void processAdditionalOptions(){}

	@Override
	public String getArgumentList(){
		return "[<jobfile_1>|<job_url_1>] [<jobfile_2>|<job_url_2>] ...";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_JOBS;
	}

	@Override
	public String getSynopsis(){
		return "The job(s) are referenced either by job files as written " +
				"by the 'run' command or as URLs.";
	}

	protected abstract void performCommand(List<JobClient>jobClients);

}
