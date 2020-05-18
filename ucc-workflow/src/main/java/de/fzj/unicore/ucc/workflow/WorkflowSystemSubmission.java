package de.fzj.unicore.ucc.workflow;

import org.apache.commons.cli.OptionBuilder;

import de.fzj.unicore.ucc.util.UCCBuilder;
import eu.unicore.ucc.actions.ActionBase;

/**
 * contains some common functionality for dealing with the workflow system.
 *
 * @author schuller
 */
public class WorkflowSystemSubmission extends ActionBase {

	public static final String OPT_DRYRUN_LONG="dryRun";
	public static final String OPT_DRYRUN="d";

	protected String siteName;

	protected UCCBuilder builder;

	protected boolean dryRun = false;

	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SITENAME_LONG)
				.withDescription("Site name for submission")
				.withArgName("Vsite")
				.hasArg()
				.isRequired(false)
				.create(OPT_SITENAME)
		);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_FACTORY_LONG)
				.withDescription("URL or site name of storage factory to use")
				.isRequired(false)
				.hasArg()
				.withArgName("StorageFactory")
				.create(OPT_FACTORY)
		);

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_DRYRUN_LONG)
				.withDescription("Dry run, do not submit anything")
				.isRequired(false)
				.create(OPT_DRYRUN)
				);

	}

	
	@Override
	public void process(){
		super.process();
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);

		dryRun=getBooleanOption(OPT_DRYRUN_LONG, OPT_DRYRUN);
		verbose("Dry run = "+dryRun);
	}


}
