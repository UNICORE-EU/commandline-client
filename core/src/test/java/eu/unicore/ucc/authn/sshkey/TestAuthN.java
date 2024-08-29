package eu.unicore.ucc.authn.sshkey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.jupiter.api.Test;

import eu.unicore.services.rest.security.jwt.JWTUtils;
import eu.unicore.ucc.UCC;

public class TestAuthN {

	@Test
	public void testLoadAuthN() throws Exception {
		assertTrue(UCC.getAuthNMethod(new SSHKeyAuthN().getName())!=null);
		assertTrue(UCC.getAuthNMethod(new SSHKeyAuthN().getName()) instanceof SSHKeyAuthN);
	}

	@Test
	public void testSSHKeyAuthN() throws Exception {
		var a = new SSHKeyAuthN();
		var p = new Properties();
		p.setProperty("identity", "src/test/resources/certs/test_id");
		p.setProperty("username", "demouser");
		a.setProperties(p);
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		String jwt = h.getValue().split(" ")[1];
		assertEquals("demouser", JWTUtils.getIssuer(jwt));
		System.out.println("Token payload: "+ JWTUtils.getPayload(jwt).toString(2));
	}

}