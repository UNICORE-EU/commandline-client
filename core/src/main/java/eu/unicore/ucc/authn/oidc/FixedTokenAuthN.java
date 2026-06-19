package eu.unicore.ucc.authn.oidc;

import java.util.Properties;

import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.security.wsutil.client.authn.PropertiesBasedAuthenticationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.PropertiesAwareAuthn;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * Base class for authenticating using a Bearer token.
 *
 * (it also handles the case where the token is explicitly
 * given in the properties)
 *
 * @author schuller
 */
public class FixedTokenAuthN extends PropertiesBasedAuthenticationProvider 
                             implements PropertiesAwareAuthn, IAuthCallback {

	private eu.unicore.services.restclient.oidc.TokenAuthN authN;

	@Override
	public String getName() {
		return "bearer-token";
	}

	@Override
	public String getDescription()
	{
		return "Authenticate with a token given via the 'token' property.";
	}

	@Override
	public String getUsage()
	{
		StringBuilder ret = new StringBuilder();
		ret.append("The following properties can be used in the UCC preference file " +
				"to configure the "+getName()+" authentication.\n");
		ret.append("token - the token\n");
		ret.append("token-type - the token type (defaults to 'Bearer')\n");
		ret.append("\nFor configuring your trusted CAs and certificates, "
				+ "use the usual 'truststore.*' propertierefreshTokens\n");
		return ret.toString();
	}

	@Override
	public void setProperties(Properties properties) {
		this.properties = properties;
		properties.setProperty("client."+ClientProperties.PROP_SSL_AUTHN_ENABLED, "false");
		properties.setProperty("client."+ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
		this.authN = new eu.unicore.services.restclient.oidc.TokenAuthN();
		this.authN.setLogger(UCC.console);
		this.authN.setProperties(properties);
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		authN.addAuthenticationHeaders(httpMessage);
	}

}
