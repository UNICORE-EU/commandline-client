package de.fzj.unicore.ucc.admin;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.fzj.unicore.ucc.Constants;
import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.util.EmbeddedTestBase;

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
