package eu.unicore.ucc.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.runner.RunnerException;

public class TestVariousHelpers {

	@Test
	public void testDefaultMessageWriter(){
		ConsoleLogger msg=new ConsoleLogger();
		assertFalse(msg.isVerbose());
		msg.info("test");
		msg.error(new Exception("Some error for testing"), "test");
		msg.error(null, "test with null exception");
	}
	
	@Test
	public void testRunnerException(){
		RunnerException re=new RunnerException("1","some reason");
		assertEquals("1",re.getErrorCode());
		assertEquals("some reason",re.getErrorReason());
		
		RunnerException re2=new RunnerException();
		assertEquals(Runner.ERR_UNKNOWN,re2.getErrorCode());
		assertNull(re2.getErrorReason());
		
		RunnerException re3=new RunnerException("1","some reason", new Exception("some cause"));
		assertEquals("1",re3.getErrorCode());
		assertEquals("some reason",re3.getErrorReason());
		assertNotNull(re3.getCause());
	}
	
	@Test
	public void testResourceCache() throws Exception {
		ResourceCache c = ResourceCache.getInstance();
		c.put("foo", "ham", "spam");
		String e = (String)c.get("foo", "ham");
		assertEquals("spam", e);
	}
}
