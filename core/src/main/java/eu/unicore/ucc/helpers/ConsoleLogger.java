package eu.unicore.ucc.helpers;

import org.apache.logging.log4j.Logger;

import eu.unicore.uas.util.MessageWriter;
import eu.unicore.util.Log;

public class ConsoleLogger {

	private static final Logger logger = Log.getLogger("UCC", MessageWriter.class);
	
	private boolean verbose;
	
	private String prefix = "[ucc]";

	public void setPrefix(String prefix){
		this.prefix =prefix;
	}

	public void setVerbose(boolean verbose){this.verbose=verbose;}
	
	public boolean isVerbose(){return verbose;}
	
	public void error(String message, Throwable cause){
		System.err.println(prefix +" "+message);
		if(cause!=null){
			System.err.println("The root error was: "+Log.getDetailMessage(cause));
			if(verbose)cause.printStackTrace();
			else{
				System.err.println("Re-run in verbose mode (-v) to see the full error stack trace.");
			}
		}
		logger.error(message, cause);
	}

	public void message(String message) {
		System.out.println(message);
		logger.info(message);
	}

	public void verbose(String message) {
		if(verbose)System.out.println(prefix +" "+message);
		logger.debug(message);
	}

}
