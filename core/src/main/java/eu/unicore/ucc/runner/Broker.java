package eu.unicore.ucc.runner;

import java.util.Collection;

import eu.unicore.client.Endpoint;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.util.UCCBuilder;

public interface Broker {

	/**
	 * Select a matching site
	 * 
	 * @param registry - the registry to check
	 * @param secProvider - security config provider
	 * @param builder - the {@link UCCBuilder} containing the job definition and other settings
	 * @param strategy - selector in case multiple matching sites exist
	 * @return the address of a matching site (never null). If no site can be found an exception will be thrown 
	 * @throws Exception
	 */
	public Endpoint findTSSAddress(IRegistryClient registry, 
			UCCConfigurationProvider secProvider, UCCBuilder builder, SiteSelectionStrategy strategy) 
			throws Exception;
	
	/**
	 * List those sites that can run the given job
	 * 
	 * @param registry - the registry to check
	 * @param secProvider - security config provider
	 * @param builder - the {@link UCCBuilder} containing the job definition and other settings
	 * @return the addresses of matching sites (never null). If no site can be found an exception will be thrown 
	 * @throws Exception
	 */
	public Collection<Endpoint> listCandidates(IRegistryClient registry, 
			UCCConfigurationProvider secProvider, UCCBuilder builder) 
			throws Exception;
	
	
	/**
	 * allows to select the "best" broker
	 */
	public int getPriority();
	
	/**
	 * allows user to select the broker
	 */
	public String getName();
	
}
