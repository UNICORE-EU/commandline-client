package eu.unicore.ucc.actions.data;

import org.json.JSONObject;

import eu.unicore.client.core.StorageClient;

/**
 * Rename/move a remote file on the same storage
 * 
 * @author schuller
 */
public class Rename extends SMSOperation {

	private String target;
	private String source;

	@Override
	public void process() throws Exception {
		super.process();
		String[]args = getCommandLine().getArgs();
		if(args.length<3)throw new IllegalArgumentException("Must have source and target arguments!");
		source = args[args.length-2];
		target = args[args.length-1];
		StorageClient sms = getStorageClient(source);
		String sourceName = getPathAtStorage(source);
		JSONObject params = new JSONObject();
		params.put("from", sourceName);
		params.put("to", target);
		sms.executeAction("rename", params);
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

}
