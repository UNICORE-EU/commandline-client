package eu.unicore.ucc.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.lookup.Producer;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.util.Log;
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
						configurationProvider.getRESTAuthN(), addressFilter, tags);
				sp.showAll(showAll);
				addProducer(sp);
			}
		}
	}

	public static class StorageProducer implements Producer<StorageClient>{

		private final String epr;

		protected final IClientConfiguration securityProperties;
		protected final IAuthCallback auth;
		
		protected final List<Pair<String,String>>errors = new ArrayList<>();

		private AtomicInteger runCount;

		protected BlockingQueue<StorageClient> target;

		protected AddressFilter addressFilter;
		
		private final String[] tags;
		
		private boolean showAll = false;

		public StorageProducer(String epr, IClientConfiguration securityProperties, IAuthCallback auth, 
				AddressFilter addressFilter, String[] tags) {
			this.epr = epr;
			this.securityProperties = securityProperties;
			this.auth = auth;
			this.addressFilter = addressFilter;
			this.tags = tags;
		}

		@Override
		public void run() {
			try{
				handleEndpoint(epr);
			}
			catch(Exception ex){
				errors.add(new Pair<>(epr,Log.createFaultMessage("", ex)));
			}
			finally{
				runCount.decrementAndGet();
			}
		}

		private void handleEndpoint(String epr) throws Exception{
			try(var core = new CoreClient(epr, securityProperties, auth))
			{
				String storagesUrl = core.getLinkUrl("storages");
				try(var ec = new EnumerationClient(storagesUrl, securityProperties, auth))
				{
					ec.setDefaultTags(tags);
					if(showAll) {
						ec.setFilter("all");
					}
					for(String url: ec){
						if(addressFilter.accept(url)){
							StorageClient c = new StorageClient(url, securityProperties, auth);
							if(addressFilter.accept(c)) {
								target.put(c);
							}
						}
					}
				}
			}
		}

		@Override
		public void init(BlockingQueue<StorageClient> target, AtomicInteger runCount) {
			this.target=target;
			this.runCount=runCount;
		}

		public void showAll(boolean showAll) {
			this.showAll = showAll;
		}
	}

}
