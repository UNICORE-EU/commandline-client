package eu.unicore.ucc.authn.oidc;

import java.util.Properties;

import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.security.wsutil.client.authn.PropertiesBasedAuthenticationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.oidc.OIDCAgentAuthN;
import eu.unicore.services.restclient.oidc.OIDCAgentProperties;
import eu.unicore.services.restclient.oidc.OIDCAgentProxy;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.PropertiesAwareAuthn;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * Gets a Bearer token from 'oidc-agent'
 * https://github.com/indigo-dc/oidc-agent
 * 
 * Parameters see {@link OIDCAgentProperties}

 * @author schuller
 */
public class AgentAuthN extends PropertiesBasedAuthenticationProvider 
		implements PropertiesAwareAuthn, IAuthCallback {

	OIDCAgentAuthN authN;

	public AgentAuthN()
	{
		super();
	}

	@Override
	public void setProperties(Properties properties)
	{
		this.properties = properties;
		properties.setProperty("client."+ClientProperties.PROP_SSL_AUTHN_ENABLED, "false");
		properties.setProperty("client."+ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
		this.authN = new OIDCAgentAuthN();
		this.authN.setLogger(UCC.console);
		this.authN.setProperties(properties);
	}

	@Override
	public String getName() {
		return "oidc-agent";
	}

	@Override
	public String getDescription()
	{
		return "Authenticate with an OIDC token "
				+ "retrieved from the 'oidc-agent' utility. "
				+ "See https://github.com/indigo-dc/oidc-agent";
	}

	@Override
	public String getUsage()
	{
		return "The following properties can be used in the UCC preference file " +
				"to configure the oidc-agent authentication. " +
				"Many of these are optional. Refer to the manual and/or the example files.\n" +
				"\nFor configuring the oidc-agent:\n" +
				getMeta(OIDCAgentProperties.class, OIDCAgentProperties.PREFIX) +
				"\nFor configuring your trusted CAs and certificates, use the usual 'truststore.*' properties\n";
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		authN.addAuthenticationHeaders(httpMessage);
	}

	void setAgentProxy(OIDCAgentProxy ap) {
		this.authN.setAgentProxy(ap);
	}
}
