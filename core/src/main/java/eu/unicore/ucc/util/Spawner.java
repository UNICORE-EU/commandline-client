package eu.unicore.ucc.util;

import eu.unicore.ucc.Command;
import eu.unicore.ucc.UCC;

/**
 * spawns a new UCC command from an existing one
 */
public class Spawner {

	private final Command cmd;

	public Spawner(Command base, String[] args) throws Exception {
		cmd = UCC.initCommand(args, false, base.getProperties());
		if(cmd!=null){
			cmd.setProperties(base.getProperties());
			cmd.setPropertiesFile(base.getPropertiesFile());
		}
	}

	/**
	 * execute the command
	 * @throws Exception
	 */
	public void run() throws Exception {
		if(cmd!=null) {
			String pfx = UCC.console.getPrefix();
			try {
				UCC.console.setPrefix("[ucc "+cmd.getName()+"]");
				cmd.process();
				cmd.postProcess();
			}finally {
				UCC.console.setPrefix(pfx);
			}
		}
	}
}
