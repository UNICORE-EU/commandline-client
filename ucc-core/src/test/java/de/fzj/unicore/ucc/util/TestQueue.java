package de.fzj.unicore.ucc.util;

import org.junit.Test;

import junit.framework.Assert;

public class TestQueue {

	@Test
	public void testDelayedGet()throws Exception{
		Queue q=new Queue(100) {
			@Override
			protected void update() {
				try{
					add("test");
				}catch(Exception e){Assert.fail(e.getMessage());}
			}
		};
		
		String s=q.next();
		Assert.assertNull(s);
		Thread.sleep(200);
		s=q.next();
		Assert.assertNotNull(s);
		Assert.assertEquals("test", s);
	}
}