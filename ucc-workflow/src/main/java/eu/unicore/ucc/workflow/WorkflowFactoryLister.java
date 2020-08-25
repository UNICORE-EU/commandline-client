package eu.unicore.ucc.workflow;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.client.Endpoint;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.lookup.Producer;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowFactoryClient;

public class WorkflowFactoryLister extends Lister<WorkflowFactoryClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, WorkflowFactoryLister.class);
	
	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	public WorkflowFactoryLister(IRegistryClient registry, UCCConfigurationProvider configurationProvider){
		this(null, registry, configurationProvider, new AcceptAllFilter());
	}
	
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 */
	public WorkflowFactoryLister(ExecutorService executor, IRegistryClient registry, 
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
	public WorkflowFactoryLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider, AddressFilter addressFilter){
		super();
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		setExecutor(executor);
		setAddressFilter(addressFilter);
	}
	
	@Override
	public Iterator<WorkflowFactoryClient> iterator() {
		try{
			setupProducers();
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
		return super.iterator();
	}

	protected void setupProducers()throws Exception {
		addProducer(new Producer<WorkflowFactoryClient>() {
			
			private BlockingQueue<WorkflowFactoryClient> target;
			private AtomicInteger runCounter;
			
			@Override
			public void run() {
				try {
					List<Endpoint>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("WorkflowServices"));
					for(Endpoint site: sites){
						if(addressFilter.accept(site)){
							WorkflowFactoryClient c = new WorkflowFactoryClient(site, 
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
			public void init(BlockingQueue<WorkflowFactoryClient> target, AtomicInteger runCount) {
				this.target = target;
				this.runCounter = runCount;
			}
			
		});
		
	}
	
}
