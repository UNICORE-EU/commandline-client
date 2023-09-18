package eu.unicore.ucc.actions.data;

import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.io.Location;

/**
 * creates a directory on a remote SMS location 
 * 
 * @see Location
 *
 * @author schuller
 */
public class Mkdir extends SMSOperation {

	protected Location targetDesc;

	@Override
	public void process() {
		super.process();
		String target = getCommandLine().getArgs()[1];;
		StorageClient sms = getStorageClient(target);
		try {
			sms.mkdir(getPathAtStorage(target));
		} catch(Exception ex) {
			error("Can't create a directory.", ex);
			endProcessing(ERROR);
		}
		
	}
	
	@Override
	public String getName() {
		return "mkdir";
	}

	@Override
	public String getSynopsis() {
		return "Creates a directory on a remote storage.";
	}
	
	@Override
	public String getDescription(){
		return "create a directory remotely";
	}
	
	@Override
	public String getArgumentList(){
		return "<Storage-URL>/files/dir or <unicore://site/storage_name/dir";
	}

}
