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

	@Override
	public void process() throws Exception {
		super.process();
		assertArgsLength(2, "Please specify remote directory!");
		for(int i=1; i<getCommandLine().getArgs().length;i++){
			String target = getCommandLine().getArgs()[i];
			StorageClient sms = getStorageClient(target);
			sms.mkdir(getPathAtStorage(target));
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
		return "create directories remotely";
	}

}
