package eu.unicore.ucc.util;

import java.util.Properties;

import eu.unicore.ucc.Command;
import eu.unicore.ucc.UCC;

/**
 * spawns a new UCC command from an existing one
 */
public class Spawner {

	private final Command cmd;

	public Spawner(Command base, String[] args) throws Exception {
		Properties p = base.getProperties();
		cmd = UCC.initCommand(expandArgs(args,p), false, p);
		if(cmd!=null){
			cmd.setProperties(p);
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

	public static String[] expandArgs(String[] args, Properties p) {
		String[]res = new String[args.length];
		for(int i=0; i<args.length; i++) {
			res[i] = expandVariables(args[i],p);
		}
		return res;
	}

	public static String expandVariables(String var, Properties p){
		for(String key: p.stringPropertyNames()) {
			if(!var.contains(key))continue;
			if(var.contains("${"+key+"}")){
				var = var.replace("${"+key+"}", String.valueOf(p.get(key)));
			}
			else if(var.contains("$"+key)){
				var = var.replace("$"+key, String.valueOf(p.get(key)));				
			}
		}
		return var;
	}
}
