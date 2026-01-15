package eu.unicore.ucc.authn.sshkey;

import java.io.File;
import java.util.Properties;

import org.apache.hc.core5.http.HttpMessage;

import eu.emi.security.authn.x509.helpers.PasswordSupplier;
import eu.unicore.security.wsutil.client.authn.PropertiesBasedAuthenticationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.sshkey.SSHKey;
import eu.unicore.ucc.authn.CallbackUtils;
import eu.unicore.ucc.authn.PropertiesAwareAuthn;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * SSH key based authentication
 *
 * @author schuller
 */
public class SSHKeyAuthN extends PropertiesBasedAuthenticationProvider
    implements PropertiesAwareAuthn, IAuthCallback {

	private File privateKey;

	private String username;

	private SSHKey auth;

	private void setup() {
		privateKey = new File(properties.getProperty("identity"));
		username = properties.getProperty("username");
		if(!privateKey.exists()){
			throw new ConfigurationException("Private key file "+
					privateKey.getParent()+" does not exist.");
		}
		final PasswordSupplier pf = ()-> {
			String password = properties.getProperty("password");
			if(password==null) {
				return CallbackUtils.getPasswordFromUserCmd("private key", privateKey.getPath());
			}
			return password.toCharArray();
		};
		auth = new SSHKey(username, privateKey, pf);
	}

	@Override
	public void setProperties(Properties properties) {
		this.properties = properties;
		properties.setProperty("client."+ClientProperties.PROP_SSL_AUTHN_ENABLED, "false");
		properties.setProperty("client."+ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
		setup();
	}

	@Override
	public String getName() {
		return "sshkey";
	}

	@Override
	public String getDescription() {
		return "Authenticate with an SSH-style private key given via the 'identity' property.";
	}

	@Override
	public String getUsage()
	{
		StringBuilder ret = new StringBuilder();
		ret.append("The following properties can be used in the UCC preference file " +
				"to configure the "+getName()+" authentication.\n");
		ret.append("username - the remote user name\n");
		ret.append("identity - the path to the private key file\n");
		ret.append("password - password for unlocking the private key\n");
		return ret.toString();
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		auth.addAuthenticationHeaders(httpMessage);
	}
}
