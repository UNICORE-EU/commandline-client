package eu.unicore.ucc.authn;

import java.util.Properties;

import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.security.canl.PasswordCallback;
import eu.unicore.security.wsutil.client.authn.PropertiesBasedAuthenticationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.util.httpclient.ClientProperties;

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
			if(username==null) {
				username = CallbackUtils.getUsernameFromUser("REST API");
			}
			if(password==null)password = properties.getProperty("password");
			if(password==null) {
				char[]pw = callback.getPassword("REST API", "configured server-side");
				password = new String(pw);
			}
		}
		new UsernamePassword(username,password).addAuthenticationHeaders(httpMessage);
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
		ret.append("The following properties can be used in the UCC preference file ");
		ret.append("to configure username/password authentication.\n");
		ret.append("username - the username\n");
		ret.append("password - the password (will be queried if not given)\n");
		ret.append("\nFor configuring your trusted CAs and certificates, ");
		ret.append("use the usual 'truststore.*' properties\n");
		return ret.toString();
	}

}
