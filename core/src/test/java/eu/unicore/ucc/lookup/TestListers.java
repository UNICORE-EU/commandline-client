package eu.unicore.ucc.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestListers extends EmbeddedTestBase {


	@BeforeAll
	public static void setup() {
		connect();
		runDate();
	}

	@Test
	public void test_List_Jobs() throws Exception {
		String[] args=new String[]{"list-jobs", "-v", "-l",
					"--filter", "status", "eq", "SUCCESSFUL",
					"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		// only show certain fields
		args=new String[]{"list-jobs", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--fields", "submissionTime"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_List_Sites() throws Exception {
		String[] args=new String[]{"list-sites", "-v", "-l",
					"--filter", "resourceStatus", "eq", "READY",
					"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Resolve() throws Exception {
		String[] args=new String[]{"resolve", "-v",
					"unicore://TEST/HOME/foo.txt",
					"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_List_Storages() throws Exception {
		String[] args=new String[]{"list-storages", "-v", "-l",
					"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	@Test
	public void test_List_Jobs_Exec() throws Exception {
		connect();
		runDate();
		String[] args=new String[]{"list-jobs", "-v", "-l",
					"-c", "src/test/resources/conf/userprefs.embedded",
					"--execute", "rest", "get", "$_?fields=status"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
}
