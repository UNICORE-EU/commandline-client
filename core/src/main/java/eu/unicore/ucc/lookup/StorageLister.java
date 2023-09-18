package eu.unicore.ucc.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.util.Pair;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.client.lookup.Producer;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;

public class StorageLister extends Lister<StorageClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, StorageLister.class);
	
	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	private final String[] tags;
	
	public StorageLister(IRegistryClient registry, UCCConfigurationProvider configurationProvider, String[] tags){
		this(null,registry,configurationProvider,new AcceptAllFilter(), tags);
	}
	
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
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
		super();
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		this.tags = tags;
		setExecutor(executor);
		setAddressFilter(addressFilter);
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
		List<Endpoint>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
		for(Endpoint site: sites){
			if(addressFilter.accept(site)){
				addProducer(new StorageProducer(site, 
						configurationProvider.getClientConfiguration(site.getUrl()),
						configurationProvider.getRESTAuthN(), addressFilter, tags));
			}
		}
	}

	public static class StorageProducer implements Producer<StorageClient>{

		private final Endpoint epr;

		protected final IClientConfiguration securityProperties;
		protected final IAuthCallback auth;
		
		protected final List<Pair<Endpoint,String>>errors = new ArrayList<>();

		private AtomicInteger runCount;

		protected BlockingQueue<StorageClient> target;

		protected AddressFilter addressFilter;
		
		private final String[] tags;
		
		public StorageProducer(Endpoint epr, IClientConfiguration securityProperties, IAuthCallback auth, 
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
				log.debug("Processing site at {}", epr.getUrl());
				handleEndpoint(epr);
			}
			catch(Exception ex){
				errors.add(new Pair<>(epr,Log.createFaultMessage("", ex)));
			}
			finally{
				runCount.decrementAndGet();
			}
		}

		public void handleEndpoint(Endpoint epr) throws Exception{
			CoreClient core = new CoreClient(epr, securityProperties, auth);
			String storagesUrl = core.getLinkUrl("storages");
			EnumerationClient ec = new EnumerationClient(epr.cloneTo(storagesUrl), securityProperties, auth);
			ec.setDefaultTags(tags);
			for(String url: ec){
				if(addressFilter.accept(url)){
					StorageClient c = new StorageClient(epr.cloneTo(url), securityProperties, auth);
					if(addressFilter.accept(c)) {
						target.put(c);
					}
				}
			}
		}

		@Override
		public void init(BlockingQueue<StorageClient> target, AtomicInteger runCount) {
			this.target=target;
			this.runCount=runCount;
		}
	}

}
