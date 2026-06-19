package eu.unicore.ucc.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import eu.unicore.client.core.SiteClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.util.Pair;
import eu.unicore.util.httpclient.IClientConfiguration;

public class SiteLister extends Lister<SiteClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	private final List<Pair<String,String>>errors = Collections.synchronizedList(new ArrayList<>());

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

	public Collection<Pair<String,String>> getErrors(){
		return errors;
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
		List<String>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
		for(String site: sites){
			if(addressFilter.accept(site)){
				addProducer(new SiteProducer(site,
						configurationProvider.getClientConfiguration(site),
						configurationProvider.getRESTAuthN(),
						addressFilter,
						errors, null));
			}
		}
	}


	public static class SiteProducer extends AbstractProducer<SiteClient>{

		public SiteProducer(String ep, IClientConfiguration securityProperties, IAuthCallback auth, 
				AddressFilter addressFilter, Collection<Pair<String,String>>errors, String[] tags) {
			super("sites", ep, securityProperties, auth, addressFilter, errors, tags);
		}

		@Override
		protected SiteClient createClient(String url) {
			return new SiteClient(url, securityProperties, auth);
		}
	}

}
