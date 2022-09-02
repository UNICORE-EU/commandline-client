package eu.unicore.ucc.admin;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestAdminServiceCmds extends EmbeddedTestBase {

	@Test
	public void testAdminServiceInfo(){
		connect();
		String[] args=new String[]{"admin-info","-v", "-l",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testRunAdminCommand(){
		connect();
		String[] args=new String[]{"admin-runcommand","-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"mock", "foo=bar",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
}
