package eu.unicore.ucc.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestListers extends EmbeddedTestBase {

	@Test
	public void test_List_Jobs() throws Exception {
		connect();
		runDate();
		String[] args=new String[]{"list-jobs", "-v", "-l",
					"--filter", "status", "eq", "SUCCESSFUL",
					"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_List_Sites() throws Exception {
		connect();
		runDate();
		String[] args=new String[]{"list-sites", "-v", "-l",
					"--filter", "resourceStatus", "eq", "READY",
					"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Resolve() throws Exception {
		connect();
		runDate();
		String[] args=new String[]{"resolve", "-v",
					"unicore://TEST/HOME/foo.txt",
					"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

}
