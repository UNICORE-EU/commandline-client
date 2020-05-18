package de.fzj.unicore.ucc.util;

import org.junit.Test;

import de.fzj.unicore.ucc.helpers.DefaultMessageWriter;

public class TestProgressBar {

	@Test
	public void testWithSize()throws InterruptedException{
		ProgressBar p=new ProgressBar("testing the progress bar", 10, new DefaultMessageWriter());
		for(int i=1;i<=10;i++){
			p.updateTotal(i);
			Thread.sleep(10);
		}
		p.finish();
	}
	
	@Test
	public void testWithoutSize()throws InterruptedException{
		ProgressBar p=new ProgressBar("testing",-1,new DefaultMessageWriter());
		for(int i=0;i<=10;i++){
			p.updateTotal(i);
			Thread.sleep(10);
		}
		p.finish();
	}
	
	@Test
	public void testUpdateWithSize()throws InterruptedException{
		ProgressBar p=new ProgressBar("testing the progress bar", 10, new DefaultMessageWriter());
		for(int i=0;i<10;i++){
			p.update(1);
			Thread.sleep(10);
		}
		p.finish();
	}
	
}
