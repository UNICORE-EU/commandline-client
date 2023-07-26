package eu.unicore.ucc.util;

import org.junit.Test;

public class TestProgressBar {

	@Test
	public void testWithSize()throws InterruptedException{
		ProgressBar p=new ProgressBar("testing the progress bar", 10);
		for(int i=1;i<=10;i++){
			p.updateTotal(i);
			Thread.sleep(10);
		}
		p.finish();
	}
	
	@Test
	public void testWithoutSize()throws InterruptedException{
		ProgressBar p=new ProgressBar("testing",-1);
		for(int i=0;i<=10;i++){
			p.updateTotal(i);
			Thread.sleep(10);
		}
		p.finish();
	}
	
	@Test
	public void testUpdateWithSize()throws InterruptedException{
		ProgressBar p=new ProgressBar("testing the progress bar", 10);
		for(int i=0;i<10;i++){
			p.update(1);
			Thread.sleep(10);
		}
		p.finish();
	}
	
}
