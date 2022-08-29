package de.fzj.unicore.ucc.helpers;

import org.apache.logging.log4j.jul.LogManager;

/**
 * diverts logging from the JLine library to a Log4j logger,
 * preventing the (rare) JLine logging to the console.
 *
 * @author schuller
 */
public class JLineLogger {

	public static void init() {
		try{
			System.setProperty("java.util.logging.manager", LogManager.class.getName());
		}catch(Exception e) {}
	}
		
}
