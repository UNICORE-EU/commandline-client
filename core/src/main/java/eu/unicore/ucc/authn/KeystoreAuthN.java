package eu.unicore.ucc.authn;

import java.util.Properties;

/**
 * Simple extension of standard {@link eu.unicore.security.wsutil.client.authn.KeystoreAuthN},
 * providing UCC specific password callback and properties.
 *
 * @author schuller
 */
public class KeystoreAuthN extends eu.unicore.security.wsutil.client.authn.KeystoreAuthN implements PropertiesAwareAuthn {

	public KeystoreAuthN()
	{
		truststorePasswordCallback = CallbackUtils.getPasswordCallback();
	}

	@Override
	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}
}
