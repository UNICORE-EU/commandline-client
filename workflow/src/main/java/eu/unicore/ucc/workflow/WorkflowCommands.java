package eu.unicore.ucc.workflow;

import eu.unicore.ucc.Command;
import eu.unicore.ucc.ProvidedCommands;

/**
 * provides the set of commands in the UCC workflow module
 */
public class WorkflowCommands implements ProvidedCommands {

	@Override
	public Command[] getCommands() {
		return new Command[]{
				new SubmitWorkflow(),
				new WorkflowControl(),
				new WorkflowInfo(),
		};
	}

}
