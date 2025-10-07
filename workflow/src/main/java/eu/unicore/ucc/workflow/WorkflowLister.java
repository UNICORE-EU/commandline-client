package eu.unicore.ucc.workflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.EnumerationClient;
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
import eu.unicore.workflow.WorkflowClient;

public class WorkflowLister extends Lister<WorkflowClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	private final String[] tags;

	private boolean includeInternal = true;
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param tags
	 */
	public WorkflowLister(ExecutorService executor, IRegistryClient registry, 
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
	public WorkflowLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider, AddressFilter addressFilter, 
			String[] tags){
		super(executor);
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		this.tags = tags;
		setAddressFilter(addressFilter);
	}

	public void setIncludeInternal(boolean includeInternal) {
		this.includeInternal = includeInternal;
	}

	@Override
	public Iterator<WorkflowClient> iterator() {
		try{
			setupProducers();
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
		return super.iterator();
	}

	protected void setupProducers()throws Exception {
		List<String>urls = new ArrayList<>(); // for avoiding duplicates
		List<Endpoint>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("WorkflowServices"));
		for(Endpoint site: sites){
			if(addressFilter.accept(site)){
				urls.add(site.getUrl());
				var sp = new WorkflowProducer(site, 
						configurationProvider.getClientConfiguration(site.getUrl()),
						configurationProvider.getRESTAuthN(), addressFilter, tags);
				addProducer(sp);
			}
		}
		if(includeInternal) {
			sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
			for(Endpoint coreSite: sites){
				BaseServiceClient siteC = new BaseServiceClient(coreSite,
						configurationProvider.getClientConfiguration(coreSite.getUrl()),
						configurationProvider.getRESTAuthN());
				String wfServicesURL = null;
				boolean check = false;
				try{
					wfServicesURL = siteC.getLinkUrl("workflows");
				}catch(Exception ex) {
					wfServicesURL = coreSite.getUrl().replace("/rest/core", "/rest/workflows");
					check = true;
				}
				if(urls.contains(wfServicesURL))continue;
				urls.add(wfServicesURL);
				Endpoint site = new Endpoint(wfServicesURL);
				if(check)try {
					// check if this even exists
					new BaseServiceClient(site,
							configurationProvider.getClientConfiguration(site.getUrl()),
							configurationProvider.getRESTAuthN()).getProperties();
				}catch(Exception ex) {
					continue;
				}
				var sp = new WorkflowProducer(site, 
						configurationProvider.getClientConfiguration(site.getUrl()),
						configurationProvider.getRESTAuthN(), addressFilter, tags);
				addProducer(sp);
			}	
		}
	}

	public static class WorkflowProducer implements Producer<WorkflowClient>{

		private final Endpoint epr;

		protected final IClientConfiguration securityProperties;
		protected final IAuthCallback auth;
		
		protected final List<Pair<Endpoint,String>>errors = new ArrayList<>();

		private AtomicInteger runCount;

		protected BlockingQueue<WorkflowClient> target;

		protected AddressFilter addressFilter;
		
		private final String[] tags;

		public WorkflowProducer(Endpoint epr, IClientConfiguration securityProperties, IAuthCallback auth, 
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
			EnumerationClient ec = new EnumerationClient(epr, securityProperties, auth);
			ec.setDefaultTags(tags);
			for(String url: ec){
				if(addressFilter.accept(url)){
					WorkflowClient c = new WorkflowClient(epr.cloneTo(url), securityProperties, auth);
					if(addressFilter.accept(c)) {
						target.put(c);
					}
				}
			}
		}

		@Override
		public void init(BlockingQueue<WorkflowClient> target, AtomicInteger runCount) {
			this.target = target;
			this.runCount = runCount;
		}
	}

}
