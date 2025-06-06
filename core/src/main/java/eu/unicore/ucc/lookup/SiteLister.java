package eu.unicore.ucc.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
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

public class SiteLister extends Lister<SiteClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	public SiteLister(IRegistryClient registry, UCCConfigurationProvider configurationProvider){
		this(null,registry,configurationProvider,new AcceptAllFilter());
	}

	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 */
	public SiteLister(ExecutorService executor, IRegistryClient registry, UCCConfigurationProvider configurationProvider){
		this(executor,registry,configurationProvider,new AcceptAllFilter());
	}

	/**
	 * 
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param addressFilter - filter for accepting/rejecting service URLs 
	 */
	public SiteLister(ExecutorService executor, IRegistryClient registry, UCCConfigurationProvider configurationProvider, AddressFilter addressFilter){
		super(executor);
		setAddressFilter(addressFilter);
		this.registry = registry;
		this.configurationProvider = configurationProvider;
	}

	@Override
	public Iterator<SiteClient> iterator() {
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
				addProducer(new SiteProducer(site, 
						configurationProvider.getClientConfiguration(site.getUrl()),
						configurationProvider.getRESTAuthN(), 
						addressFilter));
			}
		}
	}

	public static class SiteProducer implements Producer<SiteClient>{

		private final Endpoint epr;
		private final IClientConfiguration securityProperties;
		private final IAuthCallback auth;
		private final List<Pair<Endpoint,String>>errors = new ArrayList<>();
		private final AddressFilter addressFilter;

		private AtomicInteger runCount;
		private BlockingQueue<SiteClient> target;

		public SiteProducer(Endpoint epr, IClientConfiguration securityProperties, IAuthCallback auth, AddressFilter addressFilter) {
			this.epr = epr;
			this.securityProperties = securityProperties;
			this.auth = auth;
			this.addressFilter = addressFilter;
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
			SiteFactoryClient factory = new SiteFactoryClient(epr, securityProperties, auth);
			EnumerationClient sites = factory.getSiteList();
			for(String url: sites){
				if(addressFilter.accept(url)){
					SiteClient c = new SiteClient(epr.cloneTo(url), securityProperties, auth);
					if(addressFilter.accept(c)) {
						target.put(c);
					}
				}
			}
		}

		@Override
		public void init(BlockingQueue<SiteClient> target, AtomicInteger runCount) {
			this.target=target;
			this.runCount=runCount;
		}
	}

}
