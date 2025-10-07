package eu.unicore.ucc.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.JobClient;
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

public class JobLister extends Lister<JobClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	private final String[] tags;

	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param tags
	 */
	public JobLister(ExecutorService executor, IRegistryClient registry, 
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
	public JobLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider, AddressFilter addressFilter, 
			String[] tags){
		super(executor);
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		this.tags = tags;
		setAddressFilter(addressFilter);
	}

	@Override
	public Iterator<JobClient> iterator() {
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
				var sp = new JobProducer(site, 
						configurationProvider.getClientConfiguration(site.getUrl()),
						configurationProvider.getRESTAuthN(), addressFilter, tags);
				addProducer(sp);
			}
		}
	}

	public static class JobProducer implements Producer<JobClient>{

		private final Endpoint epr;

		protected final IClientConfiguration securityProperties;
		protected final IAuthCallback auth;
		
		protected final List<Pair<Endpoint,String>>errors = new ArrayList<>();

		private AtomicInteger runCount;

		protected BlockingQueue<JobClient> target;

		protected AddressFilter addressFilter;
		
		private final String[] tags;

		public JobProducer(Endpoint epr, IClientConfiguration securityProperties, IAuthCallback auth, 
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

		private void handleEndpoint(Endpoint epr) throws Exception{
			CoreClient core = new CoreClient(epr, securityProperties, auth);
			String storagesUrl = core.getLinkUrl("jobs");
			EnumerationClient ec = new EnumerationClient(epr.cloneTo(storagesUrl), securityProperties, auth);
			ec.setDefaultTags(tags);
			for(String url: ec){
				if(addressFilter.accept(url)){
					JobClient c = new JobClient(epr.cloneTo(url), securityProperties, auth);
					if(addressFilter.accept(c)) {
						target.put(c);
					}
				}
			}
		}

		@Override
		public void init(BlockingQueue<JobClient> target, AtomicInteger runCount) {
			this.target = target;
			this.runCount = runCount;
		}
	}

}
