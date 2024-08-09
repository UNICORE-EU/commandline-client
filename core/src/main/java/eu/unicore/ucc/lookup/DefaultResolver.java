package eu.unicore.ucc.lookup;

import java.net.URI;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.lookup.AddressFilter;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.io.Location;

/**
 * resolves URIs of the form unicore://SITE/storage_id/file
 * 
 * @author schuller
 */
public class DefaultResolver implements IResolve {

	@Override
	public String synopsis() {
		return "unicore://SITE-NAME/storage_id/file_path";
	}
	
	@Override
	public Location resolve(String uri, IRegistryClient registry, UCCConfigurationProvider configurationProvider) {
		if(!uri.startsWith("unicore://"))return null;
		try{
			URI u = new URI(uri);
			final String site = u.getHost();
			String path = u.getPath();
			if(path.startsWith("/"))path=path.substring(1);
			String[] p = path.split("/",2);
			final String storage_id = p[0];
			final String file = p.length==2? p[1] : "/";
			
			StorageLister l = new StorageLister(registry, configurationProvider, null);
			l.setAddressFilter(new AddressFilter() {

				@Override
				public boolean accept(BaseServiceClient resource) throws Exception {
					return resource instanceof StorageClient
						&& resource.getEndpoint().getUrl().contains("/"+site+"/rest/core/storages/"+storage_id);
				}

				@Override
				public boolean accept(String uri) {
					return uri.contains("/"+site+"/rest/core");
				}

				@Override
				public boolean accept(Endpoint ep) {
					return accept(ep.getUrl());
				}
			});
			StorageClient c = l.iterator().next();
			if(c==null) {
				throw new IllegalArgumentException("No matching storage found for: "+uri);
			}
			String protocol = "BFT";
			if(u.getQuery()!=null) {
				String[]tok = u.getQuery().split("&");
				for(String t: tok) {
					if(t.startsWith("protocol=")) {
						protocol=t.split("protocol=")[1];
						break;
					}
				}
			}
			return new Location(c.getEndpoint().getUrl()+"/files/"+file, protocol);
		}catch(Exception ex) {}
		return null;
	}
	
}
