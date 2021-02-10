package de.fzj.unicore.ucc.authn.oidc;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * preferences for getting a token from an OIDC token endpoint

 * @author schuller
 */
public class OIDCProperties extends PropertiesHelper {
	
	private static final Logger log = Log.getLogger(Log.CONFIGURATION, OIDCProperties.class);

	public static final String PREFIX = "oidc.";

	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	
	public static final String TOKEN_ENDPOINT = "endpoint";
	public static final String CLIENT_ID = "clientID";
	public static final String CLIENT_SECRET = "clientSecret";
	public static final String AUTH_MODE = "authentication";
	public static final String GRANT_TYPE = "grantType";
	
	public static enum AuthMode {
		BASIC, POST,
	}
	
	public static final Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static
	{
		META.put(TOKEN_ENDPOINT, new PropertyMD().setMandatory().setDescription("The OIDC server endpoint for requesting a token"));
		
		META.put(USERNAME, new PropertyMD().setDescription("Username used to log in. If not given in " +
															"configuration, it will be asked interactively."));
		META.put(PASSWORD, new PropertyMD().setSecret().setDescription("Password used to log in. It is suggested " +
				"not to use this option for security reasons. If not given in configuration, " +
				"it will be asked interactively."));
		
		META.put(CLIENT_ID, new PropertyMD().setDescription("Client ID for authenticating to the OIDC server."));
		META.put(CLIENT_SECRET, new PropertyMD().setDescription("Client secret for authenticating to the OIDC server."));
		META.put(GRANT_TYPE, new PropertyMD("client_credentials").setDescription("Grant type to request."));
		META.put(AUTH_MODE, new PropertyMD("BASIC").setEnum(AuthMode.BASIC)
				.setDescription("How to authenticate (i.e. send client id/secret) to the OIDC server (BASIC or POST)."));
		
	}

	public OIDCProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}

}
