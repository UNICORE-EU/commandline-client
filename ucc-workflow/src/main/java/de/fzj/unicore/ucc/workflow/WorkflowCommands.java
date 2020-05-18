package de.fzj.unicore.ucc.workflow;

import de.fzj.unicore.ucc.Command;
import de.fzj.unicore.ucc.ProvidedCommands;

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
