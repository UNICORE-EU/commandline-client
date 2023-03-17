package eu.unicore.ucc.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class TestLocation {

	@Test
	public void testLocation() {
		String url = "https://localhost:8080/DEMO/rest/core/storages/WORK/files/foo.dat";
		Location l = new Location(url);
		assertTrue(l.isUnicoreURL());
		assertFalse(l.isRaw());
	}
	
	@Test
	public void testProtocolInLocation() {
		String url = "UFTP:https://localhost:8080/DEMO/rest/core/storages/WORK/files/foo.dat";
		Location l = new Location(url);
		assertTrue(l.isUnicoreURL());
		assertEquals("UFTP", l.getProtocol());
	}
	
	@Test
	public void testLocalURL(){
		File f=new File(System.getProperty("java.io.tmpdir"));
		String u1=f.getAbsolutePath();
		Location td=new Location(u1);
		assertEquals(u1, td.getName());
		assertNull("", td.getProtocol());
		assertNull("", td.getSmsEpr());
		assertTrue(td.isLocal());
		assertFalse(td.isUnicoreURL());
		assertFalse(td.isRaw());
	}
	
	@Test
	public void testUrlPattern(){
		String url="ftp://asdkfjlsdf";
		assertFalse(Location.isUNICORE_URL(url));
		assertTrue(Location.isRawURL(url));
		
		url="mailto:someone@somewhere?subject=Test&body=This%20is%20a%20test.";
		assertFalse(Location.isUNICORE_URL(url));
		assertTrue(Location.isRawURL(url));
		
		url="a\\:test";
		assertFalse(Location.isUNICORE_URL(url));
		assertFalse(Location.isRawURL(url));
		
		url="scp://asdkfjlsdf";
		assertFalse(Location.isUNICORE_URL(url));
		assertTrue(Location.isRawURL(url));
		
		url="http://asdkfjlsdf:123";
		assertFalse(Location.isUNICORE_URL(url));
		assertTrue(Location.isRawURL(url));
		
		url="https://asdkfjlsdf:123";
		assertFalse(Location.isUNICORE_URL(url));
		assertTrue(Location.isRawURL(url));
		
		url="BFT:https://asd:123/rest/core/storages/WORK/files/sdf";
		assertTrue(Location.isUNICORE_URL(url));
		assertFalse(Location.isRawURL(url));
		
		url="https://asd:123/SITE/rest/core/storages/WORK/files/sdf";
		assertTrue(Location.isUNICORE_URL(url));
		assertFalse(Location.isRawURL(url));
	}
	
	@Test
	public void testTypes() {
		String[] urls = {
				"BFT:https://asd:123/rest/core/storages/WORK/files/sdf",
				"https://asd:123/rest/core/storages/WORK/files/sdf",
				"BFT:https://localhost:8080/VENUS/rest/core/storages/123/files/stdout",
				"https://asd:123/rest/core/storages/WORK",
		};
		for(String url : urls) {
			Location l = new Location(url);
			assertTrue(l.isUnicoreURL());
		}
	}
}
