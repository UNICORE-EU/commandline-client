package eu.unicore.ucc.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import eu.unicore.client.core.StorageClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.util.Pair;
import eu.unicore.util.httpclient.IClientConfiguration;

public class StorageLister extends Lister<StorageClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	private final String[] tags;

	private boolean showAll = false;

	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param tags
	 */
	public StorageLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider, String[] tags){
		this(executor,registry,configurationProvider,new AcceptAllFilter(), tags);
	}

	/**
	 * 
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param addressFilter - filter for accepting/rejecting service URLs 
	 */
	public StorageLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider, AddressFilter addressFilter, 
			String[] tags){
		super(executor);
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		this.tags = tags;
		setAddressFilter(addressFilter);
	}

	public void showAll(boolean showAll) {
		this.showAll = showAll;
	}

	@Override
	public Iterator<StorageClient> iterator() {
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
		for(String site: sites){
			if(addressFilter.accept(site)){
				var sp = new StorageProducer(site, 
						configurationProvider.getClientConfiguration(site),
						configurationProvider.getRESTAuthN(), addressFilter, 
						new ArrayList<>(), tags);
				if(showAll)sp.setFilter("all");
				addProducer(sp);
			}
		}
	}

	public static class StorageProducer extends AbstractProducer<StorageClient>{

		public StorageProducer(String ep, IClientConfiguration securityProperties, IAuthCallback auth, 
				AddressFilter addressFilter, Collection<Pair<String,String>>errors, String[] tags) {
			super("storages", ep, securityProperties, auth, addressFilter, errors, tags);
		}

		@Override
		protected StorageClient createClient(String url) {
			return new StorageClient(url, securityProperties, auth);
		}
	}

}
