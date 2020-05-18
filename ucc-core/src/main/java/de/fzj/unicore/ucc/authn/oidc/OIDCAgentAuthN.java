package de.fzj.unicore.ucc.authn.oidc;

import java.io.IOException;
import java.util.Properties;

import org.json.JSONObject;

import eu.unicore.security.wsutil.client.OAuthBearerTokenOutInterceptor;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

/**
 * Gets a Bearer token from 'oidc-agent'
 * https://github.com/indigo-dc/oidc-agent
 * 
 * Parameters see {@link OIDCAgentProperties}

 * @author schuller
 */
public class OIDCAgentAuthN extends TokenBasedAuthN {
	
	public static final String NAME = "oidc-agent";
	
	protected OIDCAgentProperties oidcProperties;

	protected OIDCAgentProxy ap;
	
	public OIDCAgentAuthN()
	{
		super();
	}

	@Override
	public void setProperties(Properties properties)
	{
		super.setProperties(properties);
		this.oidcProperties = new OIDCAgentProperties(properties);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Authenticate with an OIDC token to UNICORE RESTful services. "
				+ "The token is retrieved from the 'oidc-agent' utility.\n"
				+ "See https://github.com/indigo-dc/oidc-agent";
	}
	
	@Override
	public String getUsage()
	{
		StringBuilder ret = new StringBuilder();
		ret.append("The following properties can be used in the UCC preference file " +
				"to configure the oidc-agent authentication. Many of these are optional. Refer to the " +
				"manual and/or the example files.\n");
		ret.append("\nFor configuring the oidc-agent:\n");
		ret.append(getMeta(OIDCAgentProperties.class, OIDCAgentProperties.PREFIX));
		ret.append("\nFor configuring your trusted CAs and certificates, "
				+ "use the usual 'truststore.*' properties\n");
		return ret.toString();
	}

	@Override
	public DefaultClientConfiguration getAnonymousClientConfiguration() {
		DefaultClientConfiguration dcc = super.getAnonymousClientConfiguration();
		try{
			retrieveToken(dcc);
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
		if(token!=null){
			dcc.getExtraSecurityTokens().put(OAuthBearerTokenOutInterceptor.TOKEN_KEY, token);
		}
		return dcc;
	}

	protected void retrieveToken(DefaultClientConfiguration dcc) throws Exception {
		setupOIDCAgent();
		
		String account = oidcProperties.getValue(OIDCAgentProperties.ACCOUNT);
		JSONObject request = new JSONObject();
		request.put("request", "access_token");
		request.put("account", account);
		Integer lifetime = oidcProperties.getIntValue(OIDCAgentProperties.LIFETIME);
		if(lifetime!=null){
			request.put("min_valid_period",lifetime);
		}
		String scope = oidcProperties.getValue(OIDCAgentProperties.SCOPE);
		if(scope!=null)request.put("scope", scope);
		JSONObject reply = new JSONObject(ap.send(request.toString()));
		boolean success = "success".equalsIgnoreCase(reply.getString("status"));
		if(!success){
			String error = reply.getString("error");
			throw new IOException("Error received from oidc-agent: <"+error+">");
		}
		
		token = reply.getString("access_token");
		
	}

	protected void setupOIDCAgent() throws Exception {
		if(!OIDCAgentProxy.isConnectorAvailable())throw new IOException("oidc-agent is not available");
		ap = new OIDCAgentProxy();
	}
}
