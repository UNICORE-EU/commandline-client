package eu.unicore.ucc;

import java.util.Collection;

import eu.unicore.client.Endpoint;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.ucc.authn.UCCConfigurationProvider;

/**
 * provides detailed information about a service.
 */
public interface IServiceInfoProvider {

	/**
	 * the service type (as published via the registry)
	 */
	public String getType();
	
	/**
	 * the service name (for the user)
	 */
	public String getServiceName();

	/**
	 * return user-focused pieces of information about the service. 
	 * For example, for an execution service this could be the number of 
	 * running jobs, or the number of installed applications.<br/>
	 * May return <code>null</code>
	 * @param epr - the endpoint of the service
	 * @param configurationProvider - the security config provider
	 */
	public String getServiceDetails(Endpoint epr, UCCConfigurationProvider configurationProvider);

	/**
	 * returns a list of all endpoints that this info provider can provide info on
	 * @param registry
	 */
	public default Collection<Endpoint> listEndpoints(IRegistryClient registry, UCCConfigurationProvider configurationProvider) throws Exception {
		return registry.listEntries(new RegistryClient.ServiceTypeFilter(getType()));
	}
}
