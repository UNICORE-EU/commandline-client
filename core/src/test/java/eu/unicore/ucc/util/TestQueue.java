package eu.unicore.ucc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class TestQueue {

	@Test
	public void testDelayedGet()throws Exception{
		Queue q=new Queue(100) {
			@Override
			protected void update() {
				try{
					add("test");
				}catch(Exception e){}
			}
		};
		String s=q.next();
		assertNull(s);
		Thread.sleep(200);
		s=q.next();
		assertNotNull(s);
		assertEquals("test", s);
	}
}