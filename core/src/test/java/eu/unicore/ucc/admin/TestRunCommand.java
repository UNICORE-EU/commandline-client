package eu.unicore.ucc.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestRunCommand extends EmbeddedTestBase {

	@BeforeAll
	public static void setup() {
		connect();
	}

	@Test
	public void testRunCommand(){
		String[] args=new String[]{"admin-runcommand", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"ToggleJobSubmission"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testParametrisedRunCommand(){
		String[] args=new String[]{"admin-runcommand", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"mock", "key=value", "foo=spam",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	@Test
	public void testInvokeWrongAction(){
		String[] args=new String[]{"admin-runcommand", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"no-such-action",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(1),UCC.exitCode);
	}

}
