package eu.unicore.ucc.lookup;

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
import eu.unicore.util.Pair;
import eu.unicore.util.httpclient.IClientConfiguration;

public class TransferLister extends Lister<BaseServiceClient>{

	private final IRegistryClient registry;

	private final UCCConfigurationProvider configurationProvider;

	private final String[] tags;

	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param tags
	 */
	public TransferLister(ExecutorService executor, IRegistryClient registry, 
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
	public TransferLister(ExecutorService executor, IRegistryClient registry, 
			UCCConfigurationProvider configurationProvider, AddressFilter addressFilter, 
			String[] tags){
		super(executor);
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		this.tags = tags;
		setAddressFilter(addressFilter);
	}

	@Override
	public Iterator<BaseServiceClient> iterator() {
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
				var sp = new TransferProducer(site, 
						configurationProvider.getClientConfiguration(site),
						configurationProvider.getRESTAuthN(), addressFilter,
						new ArrayList<>(), tags);
				addProducer(sp);
			}
		}
	}

	public static class TransferProducer extends AbstractProducer<BaseServiceClient>{

		public TransferProducer(String ep, IClientConfiguration securityProperties, IAuthCallback auth, 
				AddressFilter addressFilter, Collection<Pair<String,String>>errors, String[] tags) {
			super("transfers", ep, securityProperties, auth, addressFilter, errors, tags);
		}

		@Override
		protected BaseServiceClient createClient(String url) {
			return new BaseServiceClient(url, securityProperties, auth);
		}
	}

}
