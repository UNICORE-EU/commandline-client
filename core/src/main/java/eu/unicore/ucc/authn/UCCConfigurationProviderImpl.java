package eu.unicore.ucc.authn;

import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jline.reader.LineReader;

import eu.unicore.security.wsutil.client.authn.AuthenticationProvider;
import eu.unicore.security.wsutil.client.authn.ClientConfigurationProvider;
import eu.unicore.security.wsutil.client.authn.ClientConfigurationProviderImpl;
import eu.unicore.security.wsutil.client.authn.JsonSecuritySessionPersistence;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.ucc.Command;
import eu.unicore.ucc.Constants;
import eu.unicore.ucc.UCC;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;
import io.imunity.tanl.x509.RevocationParameters;
import io.imunity.tanl.x509.StoreUpdateListener;
import io.imunity.tanl.x509.ValidationError;
import io.imunity.tanl.x509.ValidationErrorListener;
import io.imunity.tanl.x509.ValidationResult;
import io.imunity.tanl.x509.X509CertChainValidatorExt;
import io.imunity.tanl.x509.helpers.BinaryCertChainValidator;
import io.imunity.tanl.x509.impl.X500NameUtils;

/**
 * UCC specific extension of {@link ClientConfigurationProvider}.
 * Configures the object in UCC way (from preferences)
 * and sets up user preferences from UCC options.
 *   
 * @author K. Benedyczak
 */
public class UCCConfigurationProviderImpl extends ClientConfigurationProviderImpl 
		implements UCCConfigurationProvider
{
	public static final String PREFERENCE_ARG_VO = "vo";
	public static final String PREFERENCE_ARG_ROLE = "role";
	public static final String PREFERENCE_ARG_UID = "uid";
	public static final String PREFERENCE_ARG_PGID = "pgid";
	public static final String PREFERENCE_ARG_SUP_GIDS = "supgids";
	public static final String PREFERENCE_ARG_ADD_OS_GIDS = "useOSgids";
	public static final String PREFERENCE_ARG = "Arg";
	public static final String PREFERENCE_ARG_HELP =
			PREFERENCE_ARG_UID + ":<val>|" +
			"group:<val>|" +
			PREFERENCE_ARG_ROLE + ":<val>|" + 
			PREFERENCE_ARG_SUP_GIDS + ":<val1,val2,...>|";

	private final Properties userProperties;

	private final Command command;

	public UCCConfigurationProviderImpl(String authnMethod, Properties userProperties, 
			Command command, boolean acceptAllCAs) throws Exception
	{
		super();
		this.userProperties = userProperties;
		this.command = command;
		acceptAll = acceptAllCAs;
		AuthenticationProvider ap = UCC.getAuthNMethod(authnMethod);
		if (ap instanceof PropertiesAwareAuthn){
			PropertiesAwareAuthn paa = (PropertiesAwareAuthn)ap;
			if(userProperties.getProperty("truststore.type")==null) {
				UCC.console.debug("Using default Java truststore.");
				userProperties.setProperty("truststore.type", "java_default");
			}
			paa.setProperties(userProperties);
		}
		setAuthnProvider(ap);
		setBasicConfiguration(configure(ap.getBaseClientConfiguration()));
		setAnonymousConfiguration(configure((DefaultClientConfiguration)ap.getAnonymousClientConfiguration()));
		setSessionsPersistence(new JsonSecuritySessionPersistence(
				getBasicClientConfiguration().useSecuritySessions(), getSessionStorageFile()));
		getSessionsPersistence().readSessionIDs(getBasicClientConfiguration().getSessionIDProvider());
		setSecurityPreferences(setupExtraAttributes());
	}

	private String getSessionStorageFile() {
		return userProperties!=null? userProperties.getProperty(Constants.SESSION_ID_FILEKEY) : null;
	}

	@Override
	public IAuthCallback getRESTAuthN() {
		if(getAuthnProvider() instanceof IAuthCallback) {
			return (IAuthCallback)getAuthnProvider();
		}
		else return null;
	}

	/**
	 * Prepares a map with security preferences, to be quickly applied when actual configuration is 
	 * assembled on demand. 
	 * The base implementation adds the preferences given via {@link Constants#OPT_SECURITY_PREFERENCES}
	 */
	private Map<String, String[]> setupExtraAttributes() {
		Map<String, String[]> securityPreferences = new HashMap<>();
		String[] vals = command.getCommandLine().getOptionValues(Constants.OPT_SECURITY_PREFERENCES);
		if (vals == null) {
			String fromFile = userProperties.getProperty(Constants.OPT_SECURITY_PREFERENCES_LONG);
			if (fromFile != null)
				vals = fromFile.split("( )+");
		}
		if (vals != null) {
			for (String val: vals)
				parseSecurityPreference(val, securityPreferences);
		}
		return securityPreferences;
	}

	private void parseSecurityPreference(String pref, Map<String, String[]> securityPreferences) {
		if (pref.startsWith(PREFERENCE_ARG_UID + ":")) {
			String val = pref.substring(PREFERENCE_ARG_UID.length() + 1);
			securityPreferences.put("uid", new String[]{val});
		} else if (pref.startsWith(PREFERENCE_ARG_PGID + ":")) {
			String val = pref.substring(PREFERENCE_ARG_PGID.length() + 1);
			securityPreferences.put("group", new String[]{val});
		} else if (pref.startsWith("group:")) {
			String val = pref.substring("group:".length());
			securityPreferences.put("group", new String[]{val});
		} else if (pref.startsWith(PREFERENCE_ARG_SUP_GIDS + ":")) {
			String val = pref.substring(PREFERENCE_ARG_SUP_GIDS.length() + 1);
			String vals[] = val.split(",");
			securityPreferences.put("supplementaryGroups", vals);
		} else if (pref.startsWith(PREFERENCE_ARG_ADD_OS_GIDS + ":")) {
			String val = pref.substring(PREFERENCE_ARG_ADD_OS_GIDS.length() + 1);
			if (!val.equals("true")&&!val.equals("false")) {
				String msg = "Value of the " + PREFERENCE_ARG_ADD_OS_GIDS + 
						" preference must be 'true' or 'false', but not '" + 
						val + "'";
				throw new IllegalArgumentException(msg);
			}
			securityPreferences.put("addDefaultGroups", new String[]{val});
		} else if (pref.startsWith(PREFERENCE_ARG_VO + ":")) {
			String val = pref.substring(PREFERENCE_ARG_VO.length() + 1);
			securityPreferences.put("selectedVirtualOrganisation", new String[]{val});
		} else if (pref.startsWith(PREFERENCE_ARG_ROLE + ":")) {
			String val = pref.substring(PREFERENCE_ARG_ROLE.length() + 1);
			securityPreferences.put("role", new String[]{val});
		} else {
			String msg = "Wrong value '" + pref + "' of the option -" + Constants.OPT_SECURITY_PREFERENCES + 
					", must have the following format: " + PREFERENCE_ARG_HELP;
			throw new IllegalArgumentException(msg);
		}
	}

	private static boolean acceptAll = false;

	private static final ArrayList<String>acceptedCAs = new ArrayList<>();

	@Override
	public IClientConfiguration getClientConfiguration(String url) throws Exception {
		return configure((DefaultClientConfiguration)super.getClientConfiguration(url));
	}

	public DefaultClientConfiguration configure(DefaultClientConfiguration config) {
		if(acceptAll){
			config.setValidator(new BinaryCertChainValidator(true)); 
		}
		else {
			config.setValidator(new DelegateValidator(config.getValidator()));
		}
		return config;
	}

	// TODO functionality to handle unknown CAs should be available via secutils 
	public static class DelegateValidator implements X509CertChainValidatorExt {

		final X509CertChainValidatorExt validator;

		public DelegateValidator(X509CertChainValidatorExt validator) {
			this.validator = validator;
		}

		@Override
		public ValidationResult validate(CertPath certPath) {
			X509Certificate[] chain = new X509Certificate[certPath.getCertificates().size()];
			int i = 0;
			for(Certificate c: certPath.getCertificates()) {
				chain[i]=(X509Certificate)c;
				i++;
			}
			return validate(chain);
		}

		@Override
		public ValidationResult validate(X509Certificate[] certChain) {
			ValidationResult res = validator.validate(certChain);
			if(!res.isValid()) {
				boolean ok = queryValidity(res.getPrimaryError());
				if(ok) {
					res = ValidationResult.valid(certChain!=null?Arrays.asList(certChain):null);
				}
			}
			return res;
		}

		private boolean queryValidity(ValidationError err) {
			String issuer = err.getChain()[0].getIssuerX500Principal().getName();
			String cmp = X500NameUtils.getComparableForm(issuer);
			if(!acceptedCAs.contains(cmp))
			{
				System.err.println("VALIDATION ERROR : "+err.getMessage());
				try{
					LineReader cr = UCC.getLineReader();
					String line = cr.readLine("Accept issuer <"+issuer+"> [Y/n/a]");
					acceptAll =  line.startsWith("a") || line.startsWith("A");
					boolean accept = acceptAll ||
							line.length()==0 ||
							line.startsWith("y") ||
							line.startsWith("Y");
					if(accept){
						acceptedCAs.add(cmp);
						return true;
					}
				}catch(Exception e){}
			}
			return false;
		}

		@Override
		public X509Certificate[] getTrustedIssuers() {
			return validator.getTrustedIssuers();
		}

		@Override
		public void addValidationListener(ValidationErrorListener listener) {
			validator.addValidationListener(listener);
		}

		@Override
		public void removeValidationListener(ValidationErrorListener listener) {
			validator.removeValidationListener(listener);
		}

		@Override
		public void addUpdateListener(StoreUpdateListener listener) {
			validator.addUpdateListener(listener);
		}

		@Override
		public void removeUpdateListener(StoreUpdateListener listener) {
			validator.removeUpdateListener(listener);
		}

		@Override
		public RevocationParameters getRevocationCheckingMode() {
			return validator.getRevocationCheckingMode();
		}

		@Override
		public void dispose() {
			validator.dispose();
		}

	}
}
