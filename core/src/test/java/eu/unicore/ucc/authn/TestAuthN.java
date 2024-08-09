package eu.unicore.ucc.authn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.jupiter.api.Test;

import eu.unicore.ucc.authn.oidc.TokenBasedAuthN;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestAuthN extends EmbeddedTestBase {

	@Test
	public void testNoAuthN() throws Exception {
		var a = new NoAuthN();
		a.setProperties(new Properties());
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		assertNull(m.getHeader("Authorization"));
	}

	@Test
	public void testTokenAuthN() throws Exception {
		var a = new TokenBasedAuthN();
		var p = new Properties();
		p.setProperty("token", "test123");
		a.setProperties(p);
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		assertNotNull(h);
		assertEquals("Bearer test123", h.getValue());
		// test load token from file
		FileUtils.write(new File("target","test_access_token"), "test123", "UTF-8");
		p.setProperty("token", "@target/test_access_token");
		a = new TokenBasedAuthN();
		a.setProperties(p);
		a.addAuthenticationHeaders(m);
		h = m.getHeader("Authorization");
		assertNotNull(h);
		assertEquals("Bearer test123", h.getValue());
	}

}