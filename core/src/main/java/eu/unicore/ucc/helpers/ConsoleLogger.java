package eu.unicore.ucc.helpers;

import org.apache.logging.log4j.message.ParameterizedMessage;

import eu.unicore.services.restclient.utils.UserLogger;
import eu.unicore.ucc.UCCOptions;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.util.Log;

public class ConsoleLogger implements UserLogger {

	private boolean verbose;

	private boolean debug = UCCOptions.isTrue(Utils.getProperty("UCC_DEBUG", null));
	
	private String prefix = "[ucc]";

	public void setPrefix(String prefix){
		this.prefix =prefix;
	}

	public boolean isVerbose(){ return verbose; }
	public void setVerbose(boolean verbose){ this.verbose=verbose; }
	
	public boolean isDebug(){ return debug; }
	public void setDebug(boolean debug){ this.debug=debug; }
	
	@Override
	public void error(Throwable cause, String message, Object ...params){
		System.err.println(prefix +" "+new ParameterizedMessage(message, params).getFormattedMessage());
		if(cause!=null){
			System.err.println(prefix+" The root error was: "+Log.getDetailMessage(cause));
			if(debug)cause.printStackTrace();
			else if(verbose){
				System.err.println(prefix+" Re-run in debug mode (\"UCC_DEBUG=true ucc ...\")"
						+ " to see the full error stack trace.");
			}
		}
	}

	@Override
	public void info(String msg, Object... params) {
		System.out.println(new ParameterizedMessage(msg, params).getFormattedMessage());
	}

	@Override
	public void verbose(String msg, Object... params) {
		if(verbose) {
			this.info(prefix+" "+msg, params);
		}
	}

	@Override
	public void debug(String msg, Object... params) {
		if(debug) {
			this.info(prefix+" [DEBUG] "+msg, params);
		}
	}

}
