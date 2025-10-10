package eu.unicore.ucc.authn.oidc;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.security.wsutil.client.authn.PropertiesBasedAuthenticationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.PropertiesAwareAuthn;
import eu.unicore.ucc.helpers.ConsoleLogger;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * Base class for authenticating using a Bearer token.
 *
 * (it also handles the case where the token is explicitely
 * given in the properties)
 *
 * @author schuller
 */
public class TokenBasedAuthN extends PropertiesBasedAuthenticationProvider 
                             implements PropertiesAwareAuthn, IAuthCallback {
	
	protected String token = null;
	protected String refreshToken = null;
	protected ConsoleLogger msg = UCC.console;
	protected long lastRefresh;

	@Override
	public String getName() {
		return "bearer-token";
	}

	@Override
	public String getDescription()
	{
		return "Authenticate with an OIDC token given via the 'token' property.";
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
	}

	protected void retrieveToken() throws Exception {
		token = properties.getProperty("token");
		if(token==null)throw new IllegalArgumentException("No 'token' defined in properties!");
		if(token.startsWith("@")) {
			File f = new File(token.substring(1));
			token = FileUtils.readFileToString(f, "UTF-8");
		}
	}

	protected void refreshTokenIfNecessary() throws Exception {
		// NOP
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		refreshTokenIfNecessary();
		if(token==null) {
			retrieveToken();
		}
		if(token!=null) {
			String tokenType = properties.getProperty("token-type", "Bearer");
			httpMessage.setHeader("Authorization", tokenType+" "+token);
		}
	}

}
