package eu.unicore.ucc.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.services.rest.security.jwt.JWTUtils;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.shell.Shell;
import eu.unicore.ucc.lookup.Connector;
import eu.unicore.ucc.util.EmbeddedTestBase;

/**
 * Functional tests for the UCC actions. 
 * These run against an embedded UNICORE instance.
 */
public class TestGeneralActions extends EmbeddedTestBase {

	@Test
	public void test_Help(){
		UCC.main(new String[]{"help"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		UCC.main(new String[]{"help-auth"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_CommandHelp(){
		UCC.main(new String[]{"connect", "-h"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_WrongSetup(){
		UCC.main(new String[]{"connect","-k","NOSUCHAUTHN", 
				"-c", "src/test/resources/conf/userprefs.embedded",});
		assertEquals(Integer.valueOf(ActionBase.ERROR_SECURITY),UCC.exitCode);
	}

	@Test
	public void test_WrongSetup_NoRegistry(){
		UCC.main(new String[]{"connect",
				"-c", "src/test/resources/conf/userprefs.noregistry",});
		assertEquals(Integer.valueOf(ActionBase.ERROR),UCC.exitCode);
	}

	@Test
	public void test_SystemInfo() throws IOException {

		File sessions=new File("target","ucc-session-ids");
		FileUtils.deleteQuietly(sessions);

		connect();
		String[] args=new String[]{"system-info","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		args=new String[]{"system-info", "-l",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

	}

	@Test
	public void test_Shell()throws Exception{
		connect();
		String[]args=new String[]{"shell",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-f", "src/test/resources/shell_input",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		Shell shell = (Shell)UCC.lastCommand;
		assertNull(shell.getProperties().get("ham"));
		assertEquals("bar",shell.getProperties().get("foo"));
	}

	@Test
	public void test_Groovy()throws Exception{
		connect();
		String[]args=new String[]{"run-groovy",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-f", "src/test/resources/groovy/test.groovy",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}


	@Test
	public void test_Share()throws Exception{
		connect();
		String target = Connector._last_TSS;

		// show ACL
		String[]args=new String[]{"share", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				target
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertEquals(0,Share.lastNumberOfPermits);

		// add ACL entry
		args=new String[]{"share",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"read:VO:testers","-v",
				target
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		// show ACL again
		args=new String[]{"share",
				"-c", "src/test/resources/conf/userprefs.embedded",
				target
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertEquals(1,Share.lastNumberOfPermits);
		
		// clean entries
		args=new String[]{"share",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-b",
				target
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		// show ACL again
		args=new String[]{"share",
				"-c", "src/test/resources/conf/userprefs.embedded",
				target
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertEquals(0,Share.lastNumberOfPermits);
	}
	
	@Test
	public void test_IssueToken() throws Exception {
		int lifetime = 60;
		String[]args=new String[]{"issue-token",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v",
				"--lifetime", String.valueOf(lifetime),
				"--limited",
				"--renewable",
				"--inspect",
				"https://localhost:65322/rest/core/token",
		};
		UCC.main(args);
		String token = IssueToken.lastToken;
		JSONObject o = JWTUtils.getPayload(token);
		System.out.println(o.toString(2));
		assertEquals("Wrong lifetime", lifetime, o.getInt("exp")-o.getInt("iat"));
		assertEquals("Should be limited", o.optString("aud"), o.optString("iss"));
		assertEquals("Should be renewable", "true", o.optString("renewable"));
	}

}
