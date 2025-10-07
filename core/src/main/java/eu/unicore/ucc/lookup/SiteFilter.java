package eu.unicore.ucc.lookup;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.lookup.Blacklist;
import eu.unicore.client.lookup.SiteNameFilter;

public class SiteFilter implements AddressFilter {

	private final SiteNameFilter f1;
	private final Blacklist f2;

	public SiteFilter(String siteName, String[] blacklist) {
		f1 = siteName!=null ? new SiteNameFilter(siteName) : null;
		f2 = blacklist!=null && blacklist.length>0 ? new Blacklist(blacklist) : null;
	}

	@Override
	public boolean accept(BaseServiceClient resource) throws Exception {
		return accept(resource.getEndpoint().getUrl());
	}

	@Override
	public boolean accept(Endpoint ep) {
		return accept(ep.getUrl());
	}

	@Override
	public boolean accept(String ep) {
		boolean ok = true;
		if(f1!=null) {
			ok = ok && f1.accept(ep);
		}
		if(f2!=null) {
			ok = ok && f2.accept(ep);
		}
		return ok;
	}

}
