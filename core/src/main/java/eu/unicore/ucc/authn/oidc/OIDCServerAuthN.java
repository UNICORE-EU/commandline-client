package eu.unicore.ucc.authn.oidc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.StatusLine;
import org.json.JSONObject;

import eu.unicore.security.wsutil.client.authn.FilePermHelper;
import eu.unicore.ucc.authn.CallbackUtils;
import eu.unicore.ucc.authn.oidc.OIDCProperties.AuthMode;
import eu.unicore.util.Log;
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
		loadRefreshToken();
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

	protected void loadRefreshToken() {
		File tokenFile = new File(oidcProperties.getRefreshTokensFilename());
		try {
			if(tokenFile.exists()) {
				JSONObject tokens = new JSONObject(FileUtils.readFileToString(tokenFile, "UTF-8"));
				String url = oidcProperties.getValue(OIDCProperties.TOKEN_ENDPOINT);
				JSONObject token = tokens.optJSONObject(url);
				refreshToken = token.getString("refresh_token");
				msg.verbose("Loaded refresh token for <"+url+">");
			}
		} catch (Exception ex) {
			msg.verbose("Cannot load refresh token from <"+tokenFile+">");
		}
	}

	protected void storeRefreshToken() throws IOException {
		if(refreshToken==null)return;
		File tokenFile = new File(oidcProperties.getRefreshTokensFilename());
		JSONObject tokens = new JSONObject();
		String url = oidcProperties.getValue(OIDCProperties.TOKEN_ENDPOINT);
		JSONObject token = new JSONObject();
		try (FileWriter writer=new FileWriter(tokenFile)){
			token.put("refresh_token", refreshToken);
			tokens.put(url, token);
			tokens.write(writer);
			FilePermHelper.set0600(tokenFile);
		}catch(Exception e) {
			msg.verbose("Cannot store refresh token to <"+tokenFile+">");
		}
	}

	@Override
	protected void refreshTokenIfNecessary() throws Exception {
		long instant = System.currentTimeMillis() / 1000;
		if(instant < lastRefresh + oidcProperties.getIntValue(OIDCProperties.REFRESH_INTERVAL)){
			return;
		}
		lastRefresh = instant;
		msg.verbose("Refreshing token.");
		List<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("grant_type", "refresh_token"));
		params.add(new BasicNameValuePair("refresh_token", refreshToken));
		try {
			handleReply(executeCall(params));
		}catch(Exception e) {
			token = null;
			msg.verbose(Log.createFaultMessage("Token was not refreshed.", e));
		}
	}

	@Override
	protected void retrieveToken() throws Exception {
		List<BasicNameValuePair> params = new ArrayList<>();

		String grantType = oidcProperties.getValue(OIDCProperties.GRANT_TYPE);
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
		String otp = oidcProperties.getValue(OIDCProperties.OTP);
		if(otp!=null && otp.equalsIgnoreCase("QUERY")){
			otp = new String(CallbackUtils.getPasswordFromUserCmd("OIDC server", "2FA one time "));
		}
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", password));
		String scope = oidcProperties.getValue(OIDCProperties.SCOPE);
		if(scope!=null && scope.length()>0){
			params.add(new BasicNameValuePair("scope", scope));
		}
		handleReply(executeCall(params));
	}
	
	private void handleReply(JSONObject reply) throws IOException {
		token = reply.optString("access_token", null);
		refreshToken = reply.optString("refresh_token", null);
		lastRefresh = System.currentTimeMillis() / 1000;
		storeRefreshToken();
	}

	private JSONObject executeCall(List<BasicNameValuePair> params) throws Exception {

		DefaultClientConfiguration dcc = super.getAnonymousClientConfiguration();
		String url = oidcProperties.getValue(OIDCProperties.TOKEN_ENDPOINT);
		HttpPost post = new HttpPost(url);
		
		String clientID = oidcProperties.getValue(OIDCProperties.CLIENT_ID);
		String clientSecret = oidcProperties.getValue(OIDCProperties.CLIENT_SECRET);
		
		AuthMode mode = oidcProperties.getEnumValue(OIDCProperties.AUTH_MODE, AuthMode.class);
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
			msg.verbose("Retrieved new token from <"+url+">");
			
			return new JSONObject(body);
		}
	}
	
}
