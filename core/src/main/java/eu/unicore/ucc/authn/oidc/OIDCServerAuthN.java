package eu.unicore.ucc.authn.oidc;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.StatusLine;
import org.json.JSONObject;

import eu.unicore.security.wsutil.client.OAuthBearerTokenOutInterceptor;
import eu.unicore.ucc.authn.CallbackUtils;
import eu.unicore.ucc.authn.oidc.OIDCProperties.AuthMode;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;

/**
 * Gets a Bearer token from an OIDC server.
 * 
 * Parameters see {@link OIDCProperties}

 * @author schuller
 */
public class OIDCServerAuthN extends TokenBasedAuthN {
	
	protected OIDCProperties oidcProperties;
	
	public OIDCServerAuthN()
	{
		super();
	}

	@Override
	public void setProperties(Properties properties)
	{
		super.setProperties(properties);
		this.oidcProperties = new OIDCProperties(properties);
	}
	
	protected void adaptProperties(){
		token = properties.getProperty("token");
		
	}
	
	@Override
	public String getName() {
		return "oidc-server";
	}

	@Override
	public String getDescription()
	{
		return "Authenticate with an OIDC token. The token is retrieved from an OIDC server. ";
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
	
	@Override
	public DefaultClientConfiguration getAnonymousClientConfiguration() {
		DefaultClientConfiguration dcc = super.getAnonymousClientConfiguration();
		try{
			if(token==null)retrieveToken(dcc);
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
		if(token!=null){
			dcc.getExtraSecurityTokens().put(OAuthBearerTokenOutInterceptor.TOKEN_KEY, token);
		}
		return dcc;
	}
	
	protected void retrieveToken(DefaultClientConfiguration dcc) throws Exception {
		String url = oidcProperties.getValue(OIDCProperties.TOKEN_ENDPOINT);
		HttpPost post = new HttpPost(url);
		
		AuthMode mode = oidcProperties.getEnumValue(OIDCProperties.AUTH_MODE, AuthMode.class);
		String clientID = oidcProperties.getValue(OIDCProperties.CLIENT_ID);
		String clientSecret = oidcProperties.getValue(OIDCProperties.CLIENT_SECRET);
		String grantType = oidcProperties.getValue(OIDCProperties.GRANT_TYPE);
		
		List<BasicNameValuePair> params = new ArrayList<>();
		
		if(grantType!=null){
			params.add(new BasicNameValuePair("grant_type", grantType));
		}
		
		String username = oidcProperties.getValue(OIDCProperties.USERNAME);
		if(username==null){
			username = new String(CallbackUtils.getUsernameFromUser("OIDC server"));
		}
		String password = oidcProperties.getValue(OIDCProperties.PASSWORD);
		if(password==null){
			password = new String(CallbackUtils.getPasswordFromUserCmd("OIDC server", null));
		}
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", password));
		
		if(AuthMode.BASIC.equals(mode)){
			post.addHeader("Authorization", 
					"Basic "+new String(Base64.encodeBase64((clientID+":"+clientSecret).getBytes())));
		}
		else if(AuthMode.POST.equals(mode)){
			params.add(new BasicNameValuePair("client_id", clientID));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
		}
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
		post.setEntity(entity);
		HttpClient client = HttpUtils.createClient(url, dcc);
		try(ClassicHttpResponse response = client.executeOpen(null, post, HttpClientContext.create())){
			String body = "";
			try{
				body = EntityUtils.toString(response.getEntity());
			}catch(Exception ex){};

			if(response.getCode()!=200){
				throw new Exception("Error <"+new StatusLine(response)+"> from OIDC server: "+body);
			}
			JSONObject reply = new JSONObject(body);
			token = reply.optString("access_token", null);
		}
	}
	
}
