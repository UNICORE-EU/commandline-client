/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 28-07-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
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
		Location targetDesc = createLocation(target);
		return targetDesc.getName();
	}
	
	/**
	 * Creates a {@link StorageClient} for the provided target file. Processing is
	 * ended on error.
	 * @param target - UNICORE URL i.e. protocol:https://...#/file_path
	 * @return StorageClient for the target file 
	 */
	protected StorageClient getStorageClient(String target) {
		Location targetDesc = createLocation(target);
		return getStorageClient(targetDesc);
	}
	
	/**
	 * Creates a {@link StorageClient} for the provided target Location. Processing is
	 * ended on error.
	 * @param targetDesc
	 * @return StorageClient for the target
	 */
	protected StorageClient getStorageClient(Location targetDesc){
		Endpoint e = new Endpoint(targetDesc.getSmsEpr());
		try {
			return new StorageClient(e, 
					configurationProvider.getClientConfiguration(e.getUrl()),
					configurationProvider.getRESTAuthN());
		} catch(Exception ex) {
			error("Can't contact the storage service of " + targetDesc, ex);
			endProcessing(ERROR);
			//dummy
			return null;
		}
	}

}
