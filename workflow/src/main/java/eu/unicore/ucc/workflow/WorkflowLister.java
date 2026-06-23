package eu.unicore.ucc.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.lookup.AbstractProducer;
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
		List<Pair<String,String>>errors = new ArrayList<>();
		List<String>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("WorkflowServices"));
		for(String site: sites){
			if(addressFilter.accept(site)){
				urls.add(site);
				addProducer(new WorkflowProducer(site,
						configurationProvider.getClientConfiguration(site),
						configurationProvider.getRESTAuthN(), addressFilter, errors, tags));
			}
		}
		if(includeInternal) {
			sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
			for(String coreSite: sites){
				try(var siteC = new BaseServiceClient(coreSite,
						configurationProvider.getClientConfiguration(coreSite),
						configurationProvider.getRESTAuthN()))
				{
					String wfServicesURL = null;
					boolean check = false;
					try{
						wfServicesURL = siteC.getLinkUrl("workflows");
					}catch(Exception ex) {
						wfServicesURL = coreSite.replace("/rest/core", "/rest/workflows");
						check = true;
					}
					if(urls.contains(wfServicesURL))continue;
					urls.add(wfServicesURL);
					if(check){
						// check if this even exists
						try(var c = new BaseServiceClient(wfServicesURL,
								configurationProvider.getClientConfiguration(wfServicesURL),
								configurationProvider.getRESTAuthN())){
							c.getProperties();
						}catch(Exception ex) {
							continue;
						}
					}
					addProducer(new WorkflowProducer(wfServicesURL,
							configurationProvider.getClientConfiguration(wfServicesURL),
							configurationProvider.getRESTAuthN(), addressFilter,
							errors, tags));
				}
			}
		}
	}

	public static class WorkflowProducer extends AbstractProducer<WorkflowClient>{

		public WorkflowProducer(String ep, IClientConfiguration sec, IAuthCallback auth,
				AddressFilter addressFilter, Collection<Pair<String,String>>errors, String[] tags) {
			super("n/a", ep, sec, auth, addressFilter, errors, tags);
		}

		@Override
		protected String getListURL(BaseServiceClient b) throws Exception {
			return b.getEndpoint();
		}

		@Override
		protected WorkflowClient createClient(String url) {
			return new WorkflowClient(url, securityProperties, auth);
		}
	}

}
