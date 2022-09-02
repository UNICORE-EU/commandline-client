/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 27-07-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.ucc.actions.data;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.helpers.EndProcessingException;

/**
 * Changes or prints umask of a specified resource. Can be used with SMS and TSS instances.
 * 
 * @author K. Benedyczak
 */
public class Umask extends ActionBase {
	
	public static final String OPT_SET_LONG = "set";
	public static final String OPT_SET = "s";
	
	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_SET)
				.longOpt(OPT_SET_LONG)
				.desc("Set a umask to the provided value.")
				.required(false)
				.hasArg()
				.build());
	}

	@Override
	public String getName() {
		return "umask";
	}

	@Override
	public String getSynopsis() {
		return "Get or set umask of a remote storage " +
				"or a remote site (TSS). The umask is used by the UNICORE" +
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

		CommandLine cmdLine = getCommandLine(); 
		if (cmdLine.getArgs().length != 2) {
			error("Wrong number of arguments", null);
			printUsage();
			endProcessing(ERROR_CLIENT);
		}
		String url = cmdLine.getArgs()[1];
		
		String set = getOption(OPT_SET_LONG, OPT_SET);
		
		verbose("Will "+(set!=null?" set umask to <"+set+">" : "get umask")+" for service: " + url);
		
		BaseServiceClient client = createClient(url);
		properties.put(PROP_LAST_RESOURCE_URL, url);
		
		String umaskS = null;
		
		try {
			umaskS = client.getProperties().getString("umask");
		} catch(Exception ex) {
			error("Error getting umask property. It seems that the selected service doesn't " +
					"support umask setting", ex);
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





