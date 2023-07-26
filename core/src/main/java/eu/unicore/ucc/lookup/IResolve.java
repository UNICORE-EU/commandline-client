package eu.unicore.ucc.lookup;

import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.io.Location;

/**
 * maps URIs to {@link Location} instances
 * 
 * (new implementations can register via the ServiceLoader mechanism)
 * 
 * @author schuller
 */
public interface IResolve {

	/**
	 * resolve the given URI, or return <code>null</code> if URI cannot be resolved by
	 * this resolver
	 * 
	 * @param uri
	 * @param registry
	 * @param configurationProvider
	 * @return a {@link Location} object or <code>null</code>
	 */
	public Location resolve(String uri, IRegistryClient registry, UCCConfigurationProvider configurationProvider);
	
	public String synopsis();
}
