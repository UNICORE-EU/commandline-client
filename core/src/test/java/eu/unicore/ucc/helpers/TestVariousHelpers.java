package eu.unicore.ucc.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

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
	public void testResourceCache() throws Exception {
		ResourceCache c = ResourceCache.getInstance();
		c.put("foo", "ham", "spam");
		String e = (String)c.get("foo", "ham");
		assertEquals("spam", e);
	}
}
