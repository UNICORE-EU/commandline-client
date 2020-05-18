package de.fzj.unicore.ucc.helpers;

import java.util.List;

import javax.xml.namespace.QName;

import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xfire.ClientException;
import de.fzj.unicore.wsrflite.xmlbeans.client.IRegistryQuery;

/**
 * a cache for registry listServices() and listAccessibleServices() queries
 * 
 * @author schuller
 */
public class CachingRegistryClient implements IRegistryQuery{
	
	private static final String SERVICES="services-cache";
	
	private static final String ACCESSIBLE_SERVICES="accessible-services-cache";
	
	private final ResourceCache cache;
	
	private final IRegistryQuery registry;
	
	private volatile int count=0;
	
	public CachingRegistryClient(IRegistryQuery registry){
		this.registry=registry;
		this.cache=ResourceCache.getInstance();
	}
	
	/**
	 * cached
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<EndpointReferenceType> listAccessibleServices(QName porttype)
			throws Exception {
		count++;
		List<EndpointReferenceType>result=cache.getAs(ACCESSIBLE_SERVICES, porttype,List.class);
		if(result!=null){
			return result;
		}
		result=registry.listAccessibleServices(porttype);
		cache.put(ACCESSIBLE_SERVICES,porttype,result);
		return result;
	}

	/**
	 * cached
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<EndpointReferenceType> listServices(QName porttype)
			throws Exception {
		count++;
		List<EndpointReferenceType>result=cache.getAs(SERVICES,porttype,List.class);
		if(result!=null)return result;
		result=registry.listServices(porttype);
		cache.put(SERVICES,porttype,result);
		return result;
	}
	
	public int getNumberOfRegistryLookups(){
		return count;
	}

	public boolean checkConnection() throws ClientException {
		return registry.checkConnection();
	}

	public boolean checkConnection(int timeout) throws ClientException {
		return registry.checkConnection(timeout);
	}

	public String getConnectionStatus() throws ClientException {
		return registry.getConnectionStatus();
	}

	public List<EntryType> listEntries() throws Exception{
		return registry.listEntries();
	}

	public List<EndpointReferenceType> listServices(QName porttype,
			ServiceListFilter acceptFilter) throws Exception {
		return registry.listServices(porttype, acceptFilter);
	}
	
}
