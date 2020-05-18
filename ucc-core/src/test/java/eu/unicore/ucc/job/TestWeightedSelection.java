package eu.unicore.ucc.job;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import eu.unicore.ucc.runner.WeightedSelection;

public class TestWeightedSelection {

	@Test
	public void test1(){
		WeightedSelection ws=new WeightedSelection(new File("src/test/resources/conf/testweights.properties"),null);
		Set<String> sites=new HashSet<String>();
		sites.add("SITE1");
		sites.add("SITE2");
		for(int i=0;i<110;i++){
			ws.select(sites);
		}
		
		assertEquals(100,ws.getSelectionStatistics().get("SITE2").get());
		assertEquals(10,ws.getSelectionStatistics().get("SITE1").get());
	}
	
	@Test
	//test setting weight to zero
	public void test2(){
		WeightedSelection ws=new WeightedSelection(new File("src/test/resources/conf/testweights.properties"),null);
		Set<String> sites=new HashSet<String>();
		sites.add("SITE1");
		sites.add("SITE2");
		sites.add("SITE3");
		for(int i=0;i<110;i++){
			ws.select(sites);
		}
		
		assertEquals(100,ws.getSelectionStatistics().get("SITE2").get());
		assertEquals(10,ws.getSelectionStatistics().get("SITE1").get());
		assertEquals(0,ws.getSelectionStatistics().get("SITE3").get());
	}
	
	@Test
	//test setting default weight
	public void test3(){
		WeightedSelection ws=new WeightedSelection(new File("src/test/resources/conf/testweights.properties"),null);
		Set<String> sites=new HashSet<String>();
		sites.add("SITE1");
		sites.add("SITE2");
		sites.add("SITE3");
		sites.add("SITE4");
		for(int i=0;i<21;i++){
			ws.select(sites);
		}
		
		assertEquals(10,ws.getSelectionStatistics().get("SITE2").get());
		assertEquals(1,ws.getSelectionStatistics().get("SITE1").get());
		assertEquals(0,ws.getSelectionStatistics().get("SITE3").get());
		assertEquals(10,ws.getSelectionStatistics().get("SITE4").get());
	}
}
