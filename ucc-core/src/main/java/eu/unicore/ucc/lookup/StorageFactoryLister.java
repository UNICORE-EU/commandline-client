package eu.unicore.ucc.lookup;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.lookup.Producer;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.util.Log;

public class StorageFactoryLister extends Lister<StorageFactoryClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, StorageFactoryLister.class);
	
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
		super();
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		setAddressFilter(addressFilter);
		setExecutor(executor);
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
		addProducer(new Producer<StorageFactoryClient>() {
			
			private BlockingQueue<StorageFactoryClient> target;
			private AtomicInteger runCounter;
			
			@Override
			public void run() {
				try {
					List<Endpoint>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("StorageFactory"));
					for(Endpoint site: sites){
						if(addressFilter.accept(site)){
							StorageFactoryClient c = new StorageFactoryClient(site, 
									configurationProvider.getClientConfiguration(site.getUrl()),
									configurationProvider.getRESTAuthN());
							if(addressFilter.accept(c)) {
								target.add(c);
							}
						}
					}
				}catch(Exception ex) {}
				finally {
					runCounter.decrementAndGet();
				}
			}
			
			@Override
			public void init(BlockingQueue<StorageFactoryClient> target, AtomicInteger runCount) {
				this.target = target;
				this.runCounter = runCount;
			}
			
		});
		
	}
	
}
