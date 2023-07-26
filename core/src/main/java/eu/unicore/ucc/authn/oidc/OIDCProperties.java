package eu.unicore.ucc.authn.oidc;

import java.io.File;
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
	public static final String OTP = "otp";
	public static final String OTP_PARAM_NAME = "request_key_for_otp";
	public static final String REFRESH_TOKEN_FILENAME = "refreshTokenFile";
	public static final String REFRESH_INTERVAL = "refreshInterval";

	public static enum AuthMode {
		BASIC, POST,
	}

	public static final Map<String, PropertyMD> META = new HashMap<>();
	static
	{
		META.put(TOKEN_ENDPOINT, new PropertyMD().setMandatory().setDescription("The OIDC server endpoint for requesting a token"));
		
		META.put(USERNAME, new PropertyMD().setDescription("Username used to log in. If not given in " +
															"configuration, it will be asked interactively."));
		META.put(PASSWORD, new PropertyMD().setSecret().setDescription("Password used to log in. It is suggested " +
				"not to use this option for security reasons. If not given in configuration, " +
				"it will be asked interactively."));
		META.put(OTP, new PropertyMD().setDescription("Additional one-time password for two-factor authentication. "
				+ "Set this to 'QUERY' to query it interactively."));
		META.put(OTP_PARAM_NAME, new PropertyMD("otp").
				setDescription("(internal) How to send the OTP value to the server."));
		META.put(REFRESH_TOKEN_FILENAME, new PropertyMD().
				setDescription("(internal) Filename for storing the refresh token between UCC invocations."));
		META.put(REFRESH_INTERVAL, new PropertyMD("300").setInt().
				setDescription("Interval (seconds) before refreshing the token."));
		
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

	public String getRefreshTokensFilename() {
		String filename = getValue(REFRESH_TOKEN_FILENAME);
		if(filename==null) {
			filename = System.getProperty("user.home")+File.separator+".ucc"
					+File.separator+"refresh-tokens";
		}
		return filename;
	}

}
