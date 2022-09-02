package eu.unicore.ucc.actions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.EmbeddedTestBase;

/**
 * Functional tests for the UCC actions. 
 * These run against an embedded UNICORE instance.
 */
public class TestActionsNoTruststore extends EmbeddedTestBase {
	
	@Override
	protected void connect(){
		UCC.main(new String[]{"connect", "-v", "-K", "-c","src/test/resources/conf/userprefs.x509.notruststore"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testNoTrustStore(){
		UCC.loadAuthNMethods();
		connect();	
		UCC.main(new String[]{"system-info", "-v", "-K", "-l", 
				"-c","src/test/resources/conf/userprefs.notruststore"});
		
	}
}
