package eu.unicore.ucc.authn;

import eu.unicore.security.wsutil.client.authn.ClientConfigurationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * UCC specific extension of {@link ClientConfigurationProvider}
 *
 * @author K. Benedyczak
 */
public interface UCCConfigurationProvider extends ClientConfigurationProvider
{

	public IAuthCallback getRESTAuthN();

	public IClientConfiguration getClientConfiguration(String url) throws Exception;
}
