package eu.unicore.ucc.admin;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.unicore.ucc.Constants;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestRunCommand extends EmbeddedTestBase {

	@Test
	public void testRunCommand(){
		connect();
		String[] args=new String[]{"admin-runcommand", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"ToggleJobSubmission"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testParametrisedRunCommand(){
		connect();
		String[] args=new String[]{"admin-runcommand", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"mock", "key=value", "foo=spam",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	@Test
	public void testInvokeWrongAction(){
		connect();
		String[] args=new String[]{"admin-runcommand", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"no-such-action",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(Constants.ERROR_CLIENT),UCC.exitCode);
	}

}
