package de.fzj.unicore.ucc.authn;

import java.util.Properties;

import org.apache.http.HttpMessage;

import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.PasswordCallback;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.security.wsutil.client.authn.PropertiesBasedAuthenticationProvider;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

/**
 * Username/Password authentication
 *
 * @author schuller
 */
public class UsernameAuthN extends PropertiesBasedAuthenticationProvider 
		implements PropertiesAwareAuthn, IAuthCallback {

	private final PasswordCallback callback;
	
	private String username, password;
	
	public UsernameAuthN()
	{
		 callback = CallbackUtils.getPasswordCallback();
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		synchronized (this) {
			if(username==null)username = properties.getProperty("username");
			if(password==null)password = properties.getProperty("password");
			if(password==null) {
				char[]pw = callback.getPassword("REST API", "configured server-side");
				password = new String(pw);
			}
		}
		UsernamePassword up = new UsernamePassword(username,password);
		up.addAuthenticationHeaders(httpMessage);
	}
	

	@Override
	public void setProperties(Properties properties) {
		this.properties = properties;
		properties.setProperty("client."+ClientProperties.PROP_SSL_AUTHN_ENABLED, "false");
		properties.setProperty("client."+ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
	}

	public static final String NAME = "username";
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Authenticate with username and password";
	}

	@Override
	public String getUsage() {
		StringBuilder ret = new StringBuilder();
		ret.append("The following properties can be used in the UCC preference file " +
				"to configure username/password authentication.\n");
		ret.append("username - the username\n");
		ret.append("password - the password (will be queried if not given)\n");
		ret.append("\nFor configuring your trusted CAs and certificates, "
				+ "use the usual 'truststore.*' properties\n");
		return ret.toString();
	}

	@Override
	public DefaultClientConfiguration getClientConfiguration(String targetAddress, String targetDn,
			DelegationSpecification delegation) throws Exception {
		ClientProperties sp=new ClientProperties(properties, truststorePasswordCallback, 
				TruststoreProperties.DEFAULT_PREFIX, 
				CredentialProperties.DEFAULT_PREFIX, ClientProperties.DEFAULT_PREFIX);
		return sp;
	}
	
}
