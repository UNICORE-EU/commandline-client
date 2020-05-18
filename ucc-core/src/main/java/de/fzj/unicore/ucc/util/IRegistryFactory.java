package de.fzj.unicore.ucc.util;

import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.wsrflite.xmlbeans.client.IRegistryQuery;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * This class is intended to create IRegistryQuery instances
 *  
 * @author schuller
 */
public interface IRegistryFactory {

	/**
	 * create an IRegistryQuery instance connected to the given URL.
	 * If the URL does not denote a valid service this method MUST return null
	 * 
	 * @param url - the registry URL to connect to
	 * @param securityProperties - the client security properties
	 * @param msg - UCC message writer
	 * @return an {@link IRegistryQuery} instance connected to the given URL, or <code>null</code> if
	 * the URL does not denote a valid service
	 */
	public IRegistryQuery create(String url, IClientConfiguration securityProperties, MessageWriter msg);
	
}
