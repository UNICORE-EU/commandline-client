package eu.unicore.ucc.actions.data;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.io.Location;

/**
 * Adds some common methods useful for operations on SMS.
 * @author K. Benedyczak
 */
public abstract class SMSOperation extends ActionBase {

	/**
	 * Gets location an a SMS from the provided URL. See {@link Location} class for supported
	 * parameter formats.
	 * @param target
	 * @return File's path at the storage.
	 */
	protected String getPathAtStorage(String target) {
		return createLocation(target).getName();
	}

	/**
	 * Creates a {@link StorageClient} for the provided target file. Processing is
	 * ended on error.
	 * @param target - UNICORE URL i.e. protocol:https://...#/file_path
	 * @return StorageClient for the target file 
	 */
	protected StorageClient getStorageClient(String target) throws Exception {
		return getStorageClient(createLocation(target));
	}

	/**
	 * Creates a {@link StorageClient} for the provided target Location. Processing is
	 * ended on error.
	 * @param targetDesc
	 * @return StorageClient for the target
	 */
	protected StorageClient getStorageClient(Location targetDesc) throws Exception {
		return new StorageClient(new Endpoint(targetDesc.getSmsEpr()), 
				configurationProvider.getClientConfiguration(targetDesc.getSmsEpr()),
				configurationProvider.getRESTAuthN());
	}
		
	@Override
	public String getArgumentList(){
		return "<Storage-URL>/files/path or unicore://site/storage_name/path>";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_DATA;
	}

}
