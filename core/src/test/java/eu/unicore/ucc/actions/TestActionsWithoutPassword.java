package eu.unicore.ucc.actions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.util.EmbeddedTestBase;
import eu.unicore.ucc.util.KeystoreAuthNWithPasswd;

/**
 * Functional tests for the UCC actions. 
 * These run against an embedded UNICORE instance.
 */
public class TestActionsWithoutPassword extends EmbeddedTestBase {
	
	@Override
	protected void connect(){
		UCC.main(new String[]{"connect", "-k", "X509Test", "-c","src/test/resources/conf/userprefs.nopasswd"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testNoPassword(){
		KeystoreAuthNWithPasswd.QUESTIONS=0;
		UCC.loadAuthNMethods();
		connect();
		
		String[] args=new String[]{"run", "-k", "X509Test",
				"-c", "src/test/resources/conf/userprefs.nopasswd",
				"src/test/resources/jobs/date.u",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		
		args=new String[]{"rest", "GET", "-k", "X509Test",
				"-c", "src/test/resources/conf/userprefs.nopasswd",
				Run.getLastJobAddress(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		
		assertEquals(1, KeystoreAuthNWithPasswd.QUESTIONS);
	}

	@Test
	public void testShellNoPassword(){
		KeystoreAuthNWithPasswd.QUESTIONS=0;
		UCC.loadAuthNMethods();
		String[] args=new String[]{"shell", "-k", "X509Test", "-v",
				"-c", "src/test/resources/conf/userprefs.nopasswd",
				"-f", "src/test/resources/shell_input",
		};
		UCC.main(args);
		assertEquals(1, KeystoreAuthNWithPasswd.QUESTIONS);
		
	}
}
