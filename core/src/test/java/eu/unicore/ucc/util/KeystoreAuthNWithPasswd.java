package eu.unicore.ucc.util;

import java.util.Properties;

import eu.emi.security.authn.x509.ValidationErrorListener;
import eu.unicore.security.canl.CachingPasswordCallback;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.PasswordCallback;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.security.wsutil.client.authn.AuthenticationProvider;
import eu.unicore.ucc.authn.PropertiesAwareAuthn;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

public class KeystoreAuthNWithPasswd implements AuthenticationProvider, PropertiesAwareAuthn {

	public static final String X509="X509Test";

	public static int QUESTIONS = 0;
	
	private Properties properties;

	@Override
	public String getName() {
		return X509;
	}

	@Override
	public String getDescription() {
		return "Test authn provider with callback";
	}

	@Override
	public DefaultClientConfiguration getClientConfiguration(String targetAddress) throws Exception
	{
		return new ClientProperties(properties, getPasswordCallback(), 
				TruststoreProperties.DEFAULT_PREFIX, 
				CredentialProperties.DEFAULT_PREFIX, ClientProperties.DEFAULT_PREFIX);
	}

	@Override
	public DefaultClientConfiguration getAnonymousClientConfiguration() throws Exception
	{
		ClientProperties sp=new ClientProperties(properties, getPasswordCallback(), 
				TruststoreProperties.DEFAULT_PREFIX, 
				CredentialProperties.DEFAULT_PREFIX, ClientProperties.DEFAULT_PREFIX);
		return sp;
	}


	@Override
	public DefaultClientConfiguration getBaseClientConfiguration() throws Exception
	{
		return getAnonymousClientConfiguration();
	}
	
	private CachingPasswordCallback cpc=null;

	private synchronized PasswordCallback getPasswordCallback() {
		if(cpc==null){

			cpc=new CachingPasswordCallback()
			{
				@Override
				public boolean ignoreProperties() {
					return false;
				}

				@Override
				public char[] getPasswordFromUser(String protectedArtifactType, String protectedArtifactDescription) {
					QUESTIONS++;
					return "the!user".toCharArray();
				}

				@Override
				public boolean askForSeparateKeyPassword() {
					return false;
				}
			};
		}
		return cpc;
	}


	@Override
	public String getUsage()
	{
		return "unit testing only";
	}

	@Override
	public void setProperties(Properties properties)
	{
		this.properties = properties;
		
	}   
	
	@Override
	public void setValidationErrorListener(ValidationErrorListener properties){
		
	}
}
