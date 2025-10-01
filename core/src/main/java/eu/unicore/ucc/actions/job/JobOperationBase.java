package eu.unicore.ucc.actions.job;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.JobClient;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Pair;

/**
 * base class for job operations
 * 
 * @author schuller
 */
public abstract class JobOperationBase extends ActionBase {

	/**
	 * get job client and job info for the given job file or job URL
	 *
	 * @param jobDescriptor 
	 * @return pair containing {@link JobClient} and {@link UCCBuilder}
	 */
	protected Pair<JobClient,UCCBuilder> createJobClient(String jobDescriptor) throws Exception {
		Pair<JobClient,UCCBuilder>res = new Pair<>();
		
		UCCBuilder builder = createBuilder(jobDescriptor);
		res.setM2(builder);
		String url = builder.getProperty("_ucc_epr");
		if(url==null){
			throw new Exception("Job address not found! Maybe <"+jobDescriptor+"> has not been produced by ucc.");
		}
		res.setM1(new JobClient(new Endpoint(url),
					configurationProvider.getClientConfiguration(url),
					configurationProvider.getRESTAuthN()));
		return res;
	}

	/**
	 * create a {@link UCCBuilder} instance
	 * @param arg - denoting either a file, or a URL
	 */
	protected UCCBuilder createBuilder(String arg) throws Exception {
		try{
			UCCBuilder builder = null;
			File job = new File(arg);
			if(job.exists())
			{
				builder = new UCCBuilder(job, registry, configurationProvider);
				builder.setCheckLocalFiles(false);
				console.debug("Read job info from <{}>", arg);
			}
			else{
				console.debug("Accessing job at <{}>", arg);
				builder = new UCCBuilder(registry, configurationProvider);
				builder.setCheckLocalFiles(false);
				builder.setProperty("_ucc_epr", arg);
			}
			return builder;
		}catch(Exception e){
			throw new Exception("Can't use <"+arg+">.", e);
		}
	}

	@Override
	public void process() throws Exception {
		super.process();
		processAdditionalOptions();
		List<String> args = new ArrayList<>();
		if(getCommandLine().getArgs().length==1){
			try{
				console.info("Enter job URL:");
				String arg=new BufferedReader(new InputStreamReader(System.in)).readLine();
				args.add(arg);
			}catch(Exception e){
				throw new Exception("Can't read job descriptor from stdin.",e);
			}	
		}
		else{
			for(int i=1; i<getCommandLine().getArgs().length;i++){
				args.add(getCommandLine().getArgs()[i]);
			}
		}
		List<Pair<JobClient,UCCBuilder>>jobClients = new ArrayList<>();
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

	protected abstract void performCommand(List<Pair<JobClient,UCCBuilder>>jobClients);

}
