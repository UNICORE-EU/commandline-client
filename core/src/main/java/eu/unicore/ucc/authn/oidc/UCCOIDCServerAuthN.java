package eu.unicore.ucc.authn.oidc;

import java.util.Properties;

import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.services.restclient.oidc.OIDCProperties;
import eu.unicore.services.restclient.oidc.OIDCServerAuthN;

/**
 * Gets a Bearer token from an OIDC server.
 * 
 * Parameters see {@link OIDCProperties}

 * @author schuller
 */
public class UCCOIDCServerAuthN extends TokenBasedAuthN {

	private OIDCServerAuthN auth;

	public UCCOIDCServerAuthN()
	{
		super();
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		auth.addAuthenticationHeaders(httpMessage);
	}

	@Override
	public void setProperties(Properties properties)
	{
		super.setProperties(properties);
		this.auth = new eu.unicore.services.restclient.oidc.OIDCServerAuthN(
				new OIDCProperties(properties),
				super.getAnonymousClientConfiguration());	
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
