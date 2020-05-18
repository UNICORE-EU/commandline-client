package de.fzj.unicore.ucc.helpers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;

import jline.internal.Log;

/**
 * diverts logging from the JLine library to a Log4j logger,
 * preventing the (rare) JLine logging to the console.
 *
 * @author schuller
 */
public class JLineLogger extends PrintStream {

	private static final Logger logger = eu.unicore.util.Log.getLogger("ucc",JLineLogger.class);
	
	public static void init(){
		Log.setOutput(new JLineLogger());
	}
	
	private static final ByteArrayOutputStream os = new ByteArrayOutputStream();
			
	public JLineLogger(){
		super(os);
	}

	@Override
	public void flush(){
		super.flush();
		try{
			logger.debug(os.toString());
			os.reset();
		}catch(Exception ex){}
	}
	
	
}
