package de.fzj.unicore.ucc.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import de.fzj.unicore.ucc.Constants;
import de.fzj.unicore.ucc.UCC;
import eu.unicore.bugsreporter.annotation.RegressionTest;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.httpclient.IClientConfiguration;

public class TestActionBase {

	public static Properties getInsecureProperties() {
		Properties p=new Properties();
		p.put(ClientProperties.DEFAULT_PREFIX + ClientProperties.PROP_SSL_ENABLED, "false");
		p.put(ClientProperties.DEFAULT_PREFIX + ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
		return p;
	}
	

	@RegressionTest(url="https://sourceforge.net/tracker/?func=detail&aid=3371018&group_id=102081&atid=633902")
	@Test
	public void testTimeoutParameters()throws Exception{
		Properties p=new Properties();
		InputStream is=new FileInputStream("src/test/resources/conf/userprefs.embedded");
		p.load(is);
		is.close();
		ActionBase cmd=(ActionBase)UCC.initCommand(new String[]{"connect","-r","http://localhost:65322/services/WRONG_SERVICE", 
				"-c", "src/test/resources/conf/userprefs.embedded"},false);
		cmd.setProperties(p);
		cmd.initConfigurationProvider();
		IClientConfiguration cp=cmd.getConfigurationProvider().getAnonymousClientConfiguration();
		assertTrue(cp.getHttpClientProperties().getSocketTimeout()>0);
		assertTrue(cp.getHttpClientProperties().getConnectionTimeout()>0);
	}

	@Test
	public void testSecurityPreferencesParsing() throws Exception {
		ActionBase cmd=(ActionBase)UCC.initCommand(new String[]{
				"connect","-r","https://localhost:65322/services/WRONG_SERVICE", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-" + Constants.OPT_SECURITY_PREFERENCES, "vo:/someVo",
				"-" + Constants.OPT_SECURITY_PREFERENCES, "role:someRole",
				"-" + Constants.OPT_SECURITY_PREFERENCES, "uid:someUid",
				"-" + Constants.OPT_SECURITY_PREFERENCES, "pgid:someGid",
				"-" + Constants.OPT_SECURITY_PREFERENCES, "useOSgids:false",
				"-" + Constants.OPT_SECURITY_PREFERENCES, "supgids:someSupGid1,someSupGid2",
				}, false);
		verifyCommandPref(cmd, "src/test/resources/conf/userprefs.embedded");

		ActionBase cmd2=(ActionBase)UCC.initCommand(new String[]{
				"connect","-r","https://localhost:65322/services/WRONG_SERVICE", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG, "vo:/someVo",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG, "role:someRole",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG, "uid:someUid",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG, "pgid:someGid",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG, "useOSgids:false",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG, "supgids:someSupGid1,someSupGid2",
				}, false);
		verifyCommandPref(cmd2, "src/test/resources/conf/userprefs.embedded");

		ActionBase cmd3=(ActionBase)UCC.initCommand(new String[]{
				"connect","-r","https://localhost:65322/services/WRONG_SERVICE", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG + "=vo:/someVo",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG + "=role:someRole",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG + "=uid:someUid",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG + "=pgid:someGid",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG + "=useOSgids:false",
				"--" + Constants.OPT_SECURITY_PREFERENCES_LONG + "=supgids:someSupGid1,someSupGid2",
				}, false);
		verifyCommandPref(cmd3, "src/test/resources/conf/userprefs.embedded");
		
		ActionBase cmd4=(ActionBase)UCC.initCommand(new String[]{
				"connect","-r","https://localhost:65322/services/WRONG_SERVICE", 
				"-c", "src/test/resources/conf/userprefs-withSecPrefs.embedded"
				}, false);
		verifyCommandPref(cmd4, "src/test/resources/conf/userprefs-withSecPrefs.embedded");
	}
	
	private void verifyCommandPref(ActionBase cmd, String propertiesLoc) throws Exception {
		Properties props = new Properties();
		props.load(new FileInputStream(propertiesLoc));
		cmd.setProperties(props);
		cmd.initConfigurationProvider();
		Map<String, String[]> pref = cmd.getConfigurationProvider().getSecurityPreferences();
		assertEquals("/someVo", pref.get("selectedVirtualOrganisation")[0]);
		assertEquals("someRole", pref.get("role")[0]);
		assertEquals("someUid", pref.get("xlogin")[0]);
		assertEquals("someGid", pref.get("group")[0]);
		assertEquals("false", pref.get("addDefaultGroups")[0]);
		assertEquals("someSupGid1", pref.get("supplementaryGroups")[0]);
		assertEquals("someSupGid2", pref.get("supplementaryGroups")[1]);
	}

}
