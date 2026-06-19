package eu.unicore.ucc.lookup;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Producer;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.util.httpclient.IClientConfiguration;

public abstract class AbstractProducer<T> implements Producer<T>{

	protected final String ep;
	private final String targetLink;
	protected final IClientConfiguration securityProperties;
	protected final IAuthCallback auth;
	private final AddressFilter addressFilter;
	private final Collection<Pair<String,String>>errors;
	private final String[] tags;
	private String filter;

	private BlockingQueue<T> target;
	private AtomicInteger runCounter;

	public AbstractProducer(String targetLink,
			String ep, IClientConfiguration securityProperties, IAuthCallback auth,
			AddressFilter addressFilter, Collection<Pair<String,String>>errors,
			String [] tags)
	{
		this.ep = ep;
		this.targetLink = targetLink;
		this.securityProperties = securityProperties;
		this.auth = auth;
		this.addressFilter = addressFilter;
		this.errors = errors;
		this.tags = tags;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	@Override
	public void run() {
		try{
			handleEndpoint();
		}
		catch(Exception ex){
			errors.add(new Pair<>(ep,Log.createFaultMessage("", ex)));
		}
		finally{
			runCounter.decrementAndGet();
		}
	}

	private void handleEndpoint() throws Exception {
		try(var c = new BaseServiceClient(ep, securityProperties, auth))
		{
			String factoriesEp = c.getLinkUrl(targetLink);
			try(var ec = new EnumerationClient(factoriesEp, securityProperties, auth))
			{
				ec.setDefaultTags(tags);
				if(filter!=null)ec.setFilter(filter);
				for(String child: ec) {
					if(addressFilter.accept(child)) {
						T f = createClient(child);
						if(addressFilter.accept((BaseServiceClient)f)) {
							target.offer(f);
						}
					}
				}
			}
		}
	}

	@Override
	public void init(BlockingQueue<T> target, AtomicInteger runCount) {
		this.target = target;
		this.runCounter = runCount;
	}

	protected abstract T createClient(String url);
}
