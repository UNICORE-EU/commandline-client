package eu.unicore.ucc.workflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;

import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.lookup.Producer;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.workflow.WorkflowFactoryClient;

public class WorkflowFactoryLister extends Lister<WorkflowFactoryClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	private final boolean includeInternal;

	public WorkflowFactoryLister(IRegistryClient registry, UCCConfigurationProvider configurationProvider,
			boolean includeInternal){
		this(registry, configurationProvider, includeInternal, null, UCC.executor);
	}

	/**
	 * @param registry
	 * @param configurationProvider
	 * @param includeInternal - whether to also list UNICORE/X-internal workflow engines
	 * @param addressFilter - (optional) filter for accepting/rejecting service URLs
	 * @param executor - (optional)
	 * 
	 */
	public WorkflowFactoryLister(IRegistryClient registry,
			UCCConfigurationProvider configurationProvider,
			boolean includeInternal, AddressFilter addressFilter,
			ExecutorService executor){
		super(executor);
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		this.includeInternal = includeInternal;
		if(addressFilter!=null)setAddressFilter(addressFilter);
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
		addProducer(new Producer<>() {
			private BlockingQueue<WorkflowFactoryClient> target;
			private AtomicInteger runCounter;

			@Override
			public void run() {
				try {
					List<String>urls = new ArrayList<>();
					List<String>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("WorkflowServices"));
					for(String site: sites){
						if(urls.contains(site))continue;
						urls.add(site);
						checkAndAdd(site, false);
					}
					if(includeInternal) {
						sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
						for(String coreSite: sites){
							String site = coreSite.replace("/rest/core", "/rest/workflows");
							if(urls.contains(site))continue;
							urls.add(site);
							checkAndAdd(site, true);
						}
					}
				}catch(Exception ex) {}
				finally {
					runCounter.decrementAndGet();
				}
			}

			private void checkAndAdd(String site, boolean checkExists) throws Exception {
				if(addressFilter.accept(site)){
					WorkflowFactoryClient c = new WorkflowFactoryClient(site, 
							configurationProvider.getClientConfiguration(site),
							configurationProvider.getRESTAuthN());
					if(checkExists)try {
						c.getProperties();
					}catch(Exception ex) {
						IOUtils.closeQuietly(c);
						return;
					}
					if(addressFilter.accept(c)) {
						target.add(c);
					}
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
