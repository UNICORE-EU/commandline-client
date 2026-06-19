package eu.unicore.ucc.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.util.Pair;
import eu.unicore.util.httpclient.IClientConfiguration;

public class StorageFactoryLister extends Lister<StorageFactoryClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	public StorageFactoryLister(IRegistryClient registry, UCCConfigurationProvider configurationProvider, String[] tags){
		this(null, registry, configurationProvider, new AcceptAllFilter());
	}

	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 */
	public StorageFactoryLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider){
		this(executor,registry,configurationProvider,new AcceptAllFilter());
	}

	/**
	 * 
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param addressFilter - filter for accepting/rejecting service URLs 
	 */
	public StorageFactoryLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider, AddressFilter addressFilter){
		super(executor);
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		setAddressFilter(addressFilter);
	}

	@Override
	public Iterator<StorageFactoryClient> iterator() {
		try{
			setupProducers();
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
		return super.iterator();
	}

	protected void setupProducers()throws Exception {
		List<String>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
		for(final String site: sites){
			addProducer(new StorageFactoryProducer(site,
					configurationProvider.getClientConfiguration(site),
					configurationProvider.getRESTAuthN(), addressFilter,
					new ArrayList<>(), null));
		}
	}

	public static class StorageFactoryProducer extends AbstractProducer<StorageFactoryClient>{

		public StorageFactoryProducer(String ep, IClientConfiguration securityProperties, IAuthCallback auth, 
				AddressFilter addressFilter, Collection<Pair<String,String>>errors, String[] tags) {
			super("storagefactories", ep, securityProperties, auth, addressFilter, errors, tags);
		}

		@Override
		protected StorageFactoryClient createClient(String url) {
			return new StorageFactoryClient(url, securityProperties, auth);
		}
	}

}
