package eu.unicore.ucc.actions.data;

import org.json.JSONObject;

import eu.unicore.client.core.StorageClient;

/**
 * Rename/move a remote file on the same storage
 * 
 * @author schuller
 */
public class Rename extends SMSOperation {

	protected String target;
	protected String source;

	@Override
	public void process() {
		super.process();
		String[]args = getCommandLine().getArgs();
		if(args.length<3)throw new IllegalArgumentException("Must have source and target arguments!");
		source = args[args.length-2];
		target = args[args.length-1];
		
		try{
			StorageClient sms = getStorageClient(source);
			String sourceName = getPathAtStorage(source);
			JSONObject params = new JSONObject();
			params.put("from", sourceName);
			params.put("to", target);
			sms.executeAction("rename", params);
		}catch(Exception e){
			error("Can't rename file.",e);
			endProcessing(ERROR);
		}
	}

	@Override
	public String getName() {
		return "rename";
	}

	@Override
	public String getSynopsis() {
		return "Rename/move a remote file, on the same storage.";
	}
	@Override
	public String getDescription(){
		return "rename a remote file.";
	}

	@Override
	public String getArgumentList(){
		return "<source URL> <target name>";
	}
	@Override
	public String getCommandGroup(){
		return "Data management";
	}
}
