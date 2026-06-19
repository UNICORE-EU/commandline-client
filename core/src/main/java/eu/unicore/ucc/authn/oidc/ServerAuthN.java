package eu.unicore.ucc.authn.oidc;

import java.util.Properties;

import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.security.wsutil.client.authn.PropertiesBasedAuthenticationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.oidc.OIDCProperties;
import eu.unicore.services.restclient.oidc.OIDCServerAuthN;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.CallbackUtils;
import eu.unicore.ucc.authn.PropertiesAwareAuthn;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * Gets a Bearer token from an OIDC server.
 * 
 * Parameters see {@link OIDCProperties}

 * @author schuller
 */
public class ServerAuthN extends PropertiesBasedAuthenticationProvider 
implements PropertiesAwareAuthn, IAuthCallback {

	private OIDCServerAuthN auth;

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		auth.addAuthenticationHeaders(httpMessage);
	}

	@Override
	public void setProperties(Properties properties)
	{
		this.properties = properties;
		properties.setProperty("client."+ClientProperties.PROP_SSL_AUTHN_ENABLED, "false");
		properties.setProperty("client."+ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
		OIDCProperties oidcProperties = new OIDCProperties(properties);	
		this.auth = new OIDCServerAuthN(
				oidcProperties,
				super.getAnonymousClientConfiguration());
		this.auth.setLogger(UCC.console);
		this.auth.setPasswordCallback(()->{
			return new String(CallbackUtils.getPasswordFromUserCmd("password", "OIDC account password"));
		});
		this.auth.setOTPCallback(()->{
			return new String(CallbackUtils.getPasswordFromUserCmd("MFA token", "one-time MFA token"));
		});
	}

	@Override
	public String getName() {
		return "oidc-server";
	}

	@Override
	public String getDescription()
	{
		return "Authenticate with an OIDC token retrieved from an OIDC server. ";
	}

	@Override
	public String getUsage()
	{
		return "The following properties can be used in the UCC preference file " +
				"to configure the "+getName()+" authentication. " +
				"Many of these are optional. Refer to the " +
				"manual and/or the example files.\n" +
				"\nFor configuring the access to the OIDC server:\n" +
				getMeta(OIDCProperties.class, OIDCProperties.PREFIX) +
				"\nFor configuring your trusted CAs and certificates, " +
				"use the usual 'truststore.*' properties\n";
	}
	
}
