package eu.unicore.ucc.authn.oidc;

import java.io.IOException;
import java.util.Properties;

import org.json.JSONObject;

/**
 * Gets a Bearer token from 'oidc-agent'
 * https://github.com/indigo-dc/oidc-agent
 * 
 * Parameters see {@link OIDCAgentProperties}

 * @author schuller
 */
public class OIDCAgentAuthN extends TokenBasedAuthN {

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
	protected void retrieveToken() throws Exception {
		if(ap==null)setupOIDCAgent();
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
		boolean success = "success".equalsIgnoreCase(reply.optString("status"));
		if(!success){
			String error = reply.optString("error", reply.toString());
			throw new IOException("Error received from oidc-agent: <"+error+">");
		}
		token = reply.getString("access_token");
		refreshToken = reply.optString("refresh_token", null);
	}

	@Override
	protected void refreshTokenIfNecessary() throws Exception {
		long instant = System.currentTimeMillis() / 1000;
		long interval = oidcProperties.getIntValue(OIDCAgentProperties.REFRESH_INTERVAL);
		if(instant < lastRefresh + interval){
			return;
		}
		lastRefresh = instant;
		msg.verbose("Refreshing token (after <"+interval+"> seconds.");
		retrieveToken();
	}

	protected void setupOIDCAgent() throws Exception {
		if(!OIDCAgentProxy.isConnectorAvailable())throw new IOException("oidc-agent is not available");
		ap = new OIDCAgentProxy();
	}

	public void setAgentProxy(OIDCAgentProxy ap) {
		this.ap = ap;
	}
}
