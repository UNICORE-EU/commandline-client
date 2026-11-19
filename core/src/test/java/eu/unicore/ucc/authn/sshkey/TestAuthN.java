package eu.unicore.ucc.authn.sshkey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.hamcrest.Matchers;
import org.jline.reader.LineReader;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.junit5.JUnit5Mockery;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.ucc.UCC;
import eu.unicore.util.configuration.ConfigurationException;

public class TestAuthN {

	@Test
	public void testLoadAuthN() throws Exception {
		assertTrue(UCC.getAuthNMethod(new KeyAuthN().getName())!=null);
		assertTrue(UCC.getAuthNMethod(new KeyAuthN().getName()) instanceof KeyAuthN);
	}

	@Test
	public void testSetup() throws Exception {
		var a = new KeyAuthN();
		var p = new Properties();
		p.setProperty("identity", "nosuchfile");
		p.setProperty("username", "demouser");
		var ce = assertThrows(ConfigurationException.class, ()->a.setProperties(p));
		assertTrue(ce.getMessage().contains("nosuchfile"));
	}

	@Test
	public void testSSHKeyAuthN() throws Exception {
		var a = new KeyAuthN();
		var p = new Properties();
		p.setProperty("identity", "src/test/resources/certs/test_id");
		p.setProperty("username", "demouser");
		p.setProperty("password", "test123");
		a.setProperties(p);
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		String jwt = h.getValue().split(" ")[1];
		assertEquals("demouser", JWTUtils.getIssuer(jwt));
		System.out.println("Token payload: "+ JWTUtils.getPayload(jwt).toString(2));
	}

	@Test
	public void testAskPassword() throws Exception {
		var a = new KeyAuthN();
		var p = new Properties();
		Mockery context = new JUnit5Mockery();
		LineReader lr = context.mock(LineReader.class);
		context.checking(new Expectations() {{
			oneOf(lr).readLine(with(Matchers.anyOf(any(Character.class), Matchers.nullValue())));
			will(returnValue("test123"));
		}});
		UCC.setLineReader(lr);
		p.setProperty("identity", "src/test/resources/certs/test_id");
		p.setProperty("username", "demouser");
		a.setProperties(p);
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		String jwt = h.getValue().split(" ")[1];
		assertEquals("demouser", JWTUtils.getIssuer(jwt));
		context.assertIsSatisfied();
	}

}