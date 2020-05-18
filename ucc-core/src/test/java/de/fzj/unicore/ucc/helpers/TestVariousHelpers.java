package de.fzj.unicore.ucc.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import eu.unicore.ucc.runner.Runner;
import eu.unicore.ucc.runner.RunnerException;

public class TestVariousHelpers {

	@Test
	public void testDefaultMessageWriter(){
		DefaultMessageWriter msg=new DefaultMessageWriter();
		assertFalse(msg.isVerbose());
		msg.message("test");
		msg.error("test", new Exception("Some error for testing"));
		msg.error("test with null exception", null);
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
