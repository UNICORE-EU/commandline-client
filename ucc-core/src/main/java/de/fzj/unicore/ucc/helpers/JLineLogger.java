package de.fzj.unicore.ucc.helpers;

import eu.unicore.util.Log;

/**
 * diverts logging from the JLine library to a Log4j logger,
 * preventing the (rare) JLine logging to the console.
 *
 * @author schuller
 */
public class JLineLogger {

	public static void init(){
		Log.getLoggerName("foo", JLineLogger.class);
	}
		
}
