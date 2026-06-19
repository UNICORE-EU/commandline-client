package eu.unicore.ucc.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Lister;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.util.Pair;
import eu.unicore.util.httpclient.IClientConfiguration;

public class SiteFactoryLister extends Lister<SiteFactoryClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	private final List<Pair<String,String>>errors = Collections.synchronizedList(new ArrayList<>());

	public SiteFactoryLister(IRegistryClient registry, UCCConfigurationProvider configurationProvider, String[] tags){
		this(UCC.executor, registry, configurationProvider, new AcceptAllFilter());
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
		super(executor);
		this.registry = registry;
		this.configurationProvider = configurationProvider;
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

	private void setupProducers()throws Exception {
		List<String>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
		for(final String site: sites){
			addProducer(new SiteFactoryProducer(site,
					configurationProvider.getClientConfiguration(site),
					configurationProvider.getRESTAuthN(), addressFilter, errors));
		}
	}

	public Collection<Pair<String,String>> getErrors(){
		return errors;
	}

	public static class SiteFactoryProducer extends AbstractProducer<SiteFactoryClient>{

		public SiteFactoryProducer(String ep, IClientConfiguration securityProperties,
				IAuthCallback auth, AddressFilter addressFilter,
				Collection<Pair<String,String>>errors) {
			super("factories", ep, securityProperties, auth, addressFilter, errors, null);
		}

		@Override
		protected SiteFactoryClient createClient(String url) {
			return new SiteFactoryClient(url, securityProperties, auth);
		}
	}

}
