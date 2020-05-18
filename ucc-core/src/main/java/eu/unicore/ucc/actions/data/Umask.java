/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 27-07-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.ucc.actions.data;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.json.JSONObject;

import de.fzj.unicore.ucc.helpers.EndProcessingException;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.ucc.actions.ActionBase;

/**
 * Changes or prints umask of a specified resource. Can be used with SMS and TSS instances.
 * 
 * @author K. Benedyczak
 */
public class Umask extends ActionBase {
	
	public static final String OPT_SET_LONG = "set";
	public static final String OPT_SET = "s";
	
	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SET_LONG)
				.withDescription("Set a umask to the provided value.")
				.isRequired(false)
				.hasArg()
				.create(OPT_SET)
				);
	}

	@Override
	public String getName() {
		return "umask";
	}

	@Override
	public String getSynopsis() {
		return "Get or set umask of a remote storage (SMS) " +
				"or a target system service (TSS). Umask is used by the UNICORE" +
				" server side to control permissions of newly created files." +
				" Without the '" + OPT_SET_LONG + "' parameter the current umask is printed.";
	}
	
	@Override
	public String getDescription(){
		return "get or set a file creation umask";
	}
	
	@Override
	public String getArgumentList(){
		return "<Storage-URL | TargetSystem-URL>";
	}
	
	@Override
	public String getCommandGroup(){
		return "Data management";
	}
	
	
	@Override
	public void process() {
		super.process();
		String set = getOption(OPT_SET_LONG, OPT_SET);
		if (set != null)
			verbose("Will set a umask to " + set);
		else
			verbose("Will get a umask");
		
		CommandLine cmdLine = getCommandLine(); 
		if (cmdLine.getArgs().length != 2) {
			error("Wrong number of arguments", null);
			printUsage();
			endProcessing(ERROR_CLIENT);
		}
		String url = cmdLine.getArgs()[1];
		verbose("Operating on a address: " + url);
		
		
		BaseServiceClient client = createClient(url);
		
		String umaskS;
		
		try {
			umaskS = client.getProperties().getString("umask");
		} catch(Exception ex) {
			error("Error getting WS-RP document. It seems that the selected service doesn't " +
					"support umask setting", ex);
			endProcessing(ERROR);
			return; //dummy
		}
		if (umaskS == null) {
			error("The selected service doesn't support umask setting", null);
			endProcessing(ERROR);
		}
		
		if (set == null)
			message("umask: "+umaskS);
		else {
			try {
				JSONObject setDoc = new JSONObject();
				setDoc.put("umask", set);
				JSONObject reply = client.setProperties(setDoc);
				verbose(reply.toString(2));
				if(!"OK".equals(reply.getString("umask")))throw new Exception(reply.toString(2));
			} catch (Exception e) {
				error("Error updating umask", e);
				endProcessing(ERROR);
			}
		}
	}

	
	private BaseServiceClient createClient(String url) {
		Endpoint e = new Endpoint(url);
		try {
			BaseServiceClient client = new BaseServiceClient(e, 
					configurationProvider.getClientConfiguration(url),	
					configurationProvider.getRESTAuthN());
			return client;
		} catch (Exception ee) {
			error("Can't create client for " + url, ee);
			endProcessing(ERROR);
			//dummy
			throw new EndProcessingException(ERROR);
		}
	}
}





