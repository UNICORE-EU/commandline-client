package eu.unicore.ucc.actions.job;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.uas.util.Pair;
import eu.unicore.client.Endpoint;
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
	 * get job client and job info for the given job file or job URL
	 *  
	 * @param jobDescriptor 
	 * @return pair containing {@link JobClient} and {@link UCCBuilder}
	 */
	protected Pair<JobClient,UCCBuilder> createJobClient(String jobDescriptor){
		Pair<JobClient,UCCBuilder>res = new Pair<>();
		JobClient job=null;
		UCCBuilder builder=createBuilder(jobDescriptor);
		res.setM2(builder);
		String url=builder.getProperty("epr");
		if(url==null){
			error("Job address not found! Maybe <"+jobDescriptor+"> has not been produced by ucc.",null);
			endProcessing(ERROR_CLIENT);
		}
		try{
			job = new JobClient(new Endpoint(url),
					configurationProvider.getClientConfiguration(url),
					configurationProvider.getRESTAuthN());
		}
		catch(Exception e){
			error("Can't create job client.",e);
			endProcessing(ERROR);
		}
		res.setM1(job);
		return res;
	}

	/**
	 * create a {@link UCCBuilder} instance
	 * @param arg - denoting either a file, or a URL
	 */
	protected UCCBuilder createBuilder(String arg){
		UCCBuilder builder=null;
		try{
			File job=new File(arg);
			if(job.exists())
			{
				builder=new UCCBuilder(job, registry, configurationProvider);
				builder.setCheckLocalFiles(false);
				verbose("Read job info from <"+arg+">");
			}
			else{
				verbose("Accessing job at <"+arg+">");
				builder=new UCCBuilder(registry, configurationProvider);
				builder.setCheckLocalFiles(false);
				builder.setProperty("epr", arg);
			}
		}catch(Exception e){
			error("Can't use <"+arg+">.",e);
			endProcessing(ERROR_CLIENT);
		}
		return builder;
	}

	@Override
	public void process(){
		super.process();
		processAdditionalOptions();
		
		List<String> args = new ArrayList<>();

		if(getCommandLine().getArgs().length==1){
			try{
				String arg=new BufferedReader(new InputStreamReader(System.in)).readLine();
				args.add(arg);
			}catch(Exception e){
				error("Can't read job descriptor from stdin.",e);
				endProcessing(ERROR_CLIENT);
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
		return "Job execution";
	}

	@Override
	public String getSynopsis(){
		return "The job(s) are referenced either by job files as written " +
				"by the 'run' command or as URLs.";
	}
	
	protected abstract void performCommand(List<Pair<JobClient,UCCBuilder>>jobClients);

}
