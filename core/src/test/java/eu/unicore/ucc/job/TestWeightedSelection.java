package eu.unicore.ucc.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.unicore.ucc.runner.WeightedSelection;

public class TestWeightedSelection {

	@Test
	public void test_setup() throws Exception{
		assertThrows(FileNotFoundException.class, ()->new WeightedSelection(new File("nosuchfile")));
	}

	@Test
	public void test_basic_weights() throws Exception{
		WeightedSelection ws = new WeightedSelection(new File("src/test/resources/conf/testweights.properties"));
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
	public void test_set_zero_weight()throws Exception{
		WeightedSelection ws = new WeightedSelection(new File("src/test/resources/conf/testweights.properties"));
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
	public void test_setting_default_weight()throws Exception{
		WeightedSelection ws = new WeightedSelection(new File("src/test/resources/conf/testweights.properties"));
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
