package eu.unicore.ucc.lookup;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.util.Log;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.lookup.Producer;

public class SiteFactoryLister extends Lister<SiteFactoryClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, SiteFactoryLister.class);
	
	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	public SiteFactoryLister(IRegistryClient registry, UCCConfigurationProvider configurationProvider, String[] tags){
		this(null, registry, configurationProvider, new AcceptAllFilter());
	}
	
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 */
	public SiteFactoryLister(ExecutorService executor, IRegistryClient registry, 
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
	public SiteFactoryLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider, AddressFilter addressFilter){
		super();
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		setExecutor(executor);
		setAddressFilter(addressFilter);
	}
	
	@Override
	public Iterator<SiteFactoryClient> iterator() {
		try{
			setupProducers();
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
		return super.iterator();
	}

	protected void setupProducers()throws Exception {
		addProducer(new Producer<SiteFactoryClient>() {
			
			private BlockingQueue<SiteFactoryClient> target;
			private AtomicInteger runCounter;
			
			@Override
			public void run() {
				try {
					List<Endpoint>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("TargetSystemFactory"));
					for(Endpoint site: sites){
						if(addressFilter.accept(site)){
							SiteFactoryClient c = new SiteFactoryClient(site, 
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
			public void init(BlockingQueue<SiteFactoryClient> target, AtomicInteger runCount) {
				this.target = target;
				this.runCounter = runCount;
			}
			
		});
		
	}
	
}
