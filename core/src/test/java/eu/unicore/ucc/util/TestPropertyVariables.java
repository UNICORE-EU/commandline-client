package eu.unicore.ucc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class TestPropertyVariables
{
	@Test
	public void test() throws IOException
	{
		Properties p = new Properties();
		p.setProperty("k1", "s");
		p.setProperty("k2", "s${no such property}t");
		p.setProperty("k3", "s${UCC_CONFIG}t");
		p.setProperty("k4", "${UCC_CONFIG}s${UCC_CONFIG}t${UCC_CONFIG}");
		p.setProperty("k5", "s${HOME}${HOME}t");
		
		String cwd = new File("target").getAbsolutePath();
		File tst = new File(cwd, "someFile");
		tst.createNewFile();
		PropertyVariablesResolver.substituteVariables(p, tst);
		assertEquals("s", p.getProperty("k1"));
		assertEquals("s${no such property}t", p.getProperty("k2"));
		assertEquals("s" + cwd + "t", p.getProperty("k3"));
		assertEquals(cwd+"s"+cwd+"t"+cwd, p.getProperty("k4"));
	}
	
	@Test
	public void testGetParameters() throws Exception {
		String[] args = {"a=b",
				"@src/test/resources/conf/testweights.properties",
				"xx=@src/test/resources/log4j2.properties"};
		Map<String,String>params = PropertyVariablesResolver.getParameters(args);
		assertEquals("b", params.get("a"));
		assertEquals("10",params.get("SITE2"));
		assertNotNull(params.get("xx"));
		assertTrue(params.get("xx").contains("rootLogger"));
	}
}
