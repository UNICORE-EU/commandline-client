package eu.unicore.ucc.actions.job;

import java.util.List;

import org.apache.commons.cli.Option;

import eu.unicore.client.core.JobClient;
import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Pair;

public class GetOutcome extends JobOperationBase {


	/**
	 * do not add job id prefixes to output file names 
	 */
	protected boolean brief;

	/**
	 * quiet mode (e.g. don't write job id files)
	 */
	protected boolean quiet = false;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_NOPREFIX)
				.longOpt(OPT_NOPREFIX_LONG)
				.desc("Short output file names")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_QUIET)
				.longOpt(OPT_QUIET_LONG)
				.desc("Quiet mode, don't ask for confirmation")
				.required(false)
				.build());
	}
	
	@Override
	protected void processAdditionalOptions(){
		brief=getBooleanOption(OPT_NOPREFIX_LONG, OPT_NOPREFIX);
		verbose("Adding job id to output file names = "+!brief);
		quiet = getBooleanOption(OPT_QUIET_LONG, OPT_QUIET);
		verbose("Quiet mode = " + quiet);

	}
		
	@Override
	public String getName(){
		return "get-output";
	}
	
	@Override
	public String getSynopsis(){
		return "Gets the output of UNICORE job(s). " +
				super.getSynopsis();
	}
	
	@Override
	public String getDescription(){
		return "get output files";
	}

	protected void performCommand(List<Pair<JobClient,UCCBuilder>>jobs){
		jobs.forEach( x -> getOutput(x.getM2()));
	}
	
	protected void getOutput(UCCBuilder builder){
		try{
			builder.setProperty("state", Runner.STARTED);
			builder.setProperty("Output",output.getAbsolutePath());
			builder.setProperty("KeepFinishedJob","true");
			Runner runner=new Runner(registry,configurationProvider,builder);
			runner.setBriefOutfileNames(brief);
			runner.setQuietMode(quiet);
			runner.setProperties(properties);
			runner.run();
		}
		catch(Exception e){
			error("Error getting output", e);
			endProcessing(ERROR);
		}
	}
	
	
}
