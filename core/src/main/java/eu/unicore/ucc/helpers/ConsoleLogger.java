package eu.unicore.ucc.helpers;

import org.apache.logging.log4j.Logger;

import eu.unicore.ucc.UCCOptions;
import eu.unicore.util.Log;

public class ConsoleLogger {

	private static final Logger logger = Log.getLogger("UCC", ConsoleLogger.class);
	
	private boolean verbose;

	private boolean debug = UCCOptions.isTrue(System.getenv("UCC_DEBUG"));
	
	private String prefix = "[ucc]";

	public void setPrefix(String prefix){
		this.prefix =prefix;
	}


	public boolean isVerbose(){ return verbose; }
	public void setVerbose(boolean verbose){ this.verbose=verbose; }
	
	public boolean isDebug(){ return debug; }
	public void setDebug(boolean debug){ this.debug=debug; }
	
	public void error(String message, Throwable cause){
		System.err.println(prefix +" "+message);
		if(cause!=null){
			System.err.println("The root error was: "+Log.getDetailMessage(cause));
			if(debug)cause.printStackTrace();
			else if(verbose){
				System.err.println("Re-run in debug mode (\"UCC_DEBUG=true; ucc ...\")"
						+ " to see the full error stack trace.");
			}
		}
		logger.debug(message);
	}

	public void message(String message) {
		System.out.println(message);
	}

	public void verbose(String message) {
		if(verbose)System.out.println(prefix +" "+message);
		logger.debug(message);
	}

}
