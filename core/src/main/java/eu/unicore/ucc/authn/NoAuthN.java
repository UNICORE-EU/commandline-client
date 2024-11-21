package eu.unicore.ucc.authn;

import java.util.Properties;

import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.security.wsutil.client.authn.PropertiesBasedAuthenticationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * anonymous (no) authentication
 *
 * @author schuller
 */
public class NoAuthN extends PropertiesBasedAuthenticationProvider 
		implements PropertiesAwareAuthn, IAuthCallback {

	public NoAuthN(){}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
	}

	@Override
	public void setProperties(Properties properties) {
		this.properties = properties;
		properties.setProperty("client."+ClientProperties.PROP_SSL_AUTHN_ENABLED, "false");
		properties.setProperty("client."+ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
	}

	@Override
	public String getName() {
		return "none";
	}

	@Override
	public String getDescription() {
		return "No authentication at all";
	}

	@Override
	public String getUsage() {
		StringBuilder ret = new StringBuilder();
		ret.append("No authentication will be used");
		return ret.toString();
	}

}
