package eu.unicore.ucc.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestAdminServiceCmds extends EmbeddedTestBase {

	@BeforeAll
	public static void setup() {
		connect();
	}

	@Test
	public void testAdminServiceInfo(){
		String[] args=new String[]{"admin-info","-v", "-l",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testRunAdminCommand(){
		String[] args=new String[]{"admin-runcommand","-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"mock", "foo=bar",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
}
