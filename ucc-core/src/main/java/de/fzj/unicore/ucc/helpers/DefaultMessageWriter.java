package de.fzj.unicore.ucc.helpers;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.util.MessageWriter;
import eu.unicore.util.Log;

public class DefaultMessageWriter implements MessageWriter {

	private static final Logger logger = Log.getLogger("UCC", MessageWriter.class);
	
	private final boolean verbose;
	
	public DefaultMessageWriter(){
		this.verbose=false;
	} 

	public boolean isVerbose(){return verbose;}
	
	public void error(String message, Throwable cause){
		System.err.println(message);
		if(cause!=null){
			System.err.println("The root error was: "+getDetailMessage(cause));
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
		if(verbose)System.out.println(message);
		logger.debug(message);
	}

	/**
	 * construct a (hopefully) useful error message from the root cause of an 
	 * exception
	 * @param throwable
	 * @return
	 */
	private String getDetailMessage(Throwable throwable){
		StringBuilder sb=new StringBuilder();
		Throwable cause=throwable;
		String message=null;
		String type=null;type=cause.getClass().getName();
		do{
			type=cause.getClass().getName();
			message=cause.getMessage();
			cause=cause.getCause();
		}
		while(cause!=null);
		
		if(message!=null)sb.append(type).append(": ").append(message);
		else sb.append(type).append(" (no further message available)");
		return sb.toString();
	}
}
