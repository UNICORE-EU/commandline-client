package eu.unicore.ucc.authn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.ucc.authn.oidc.OIDCAgentAuthN;
import eu.unicore.ucc.authn.oidc.OIDCAgentProxy;
import eu.unicore.ucc.authn.oidc.OIDCServerAuthN;
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
	}

	@Test
	public void testOIDCServerAuthN() throws Exception {
		var a = new OIDCServerAuthN();
		var p = new Properties();
		p.setProperty("oidc.clientID", "demouser");
		p.setProperty("oidc.clientSecret", "test123");
		p.setProperty("oidc.username", "foo");
		p.setProperty("oidc.password", "bar");
		p.setProperty("oidc.refreshTokenFile", "target/oidc_refresh_token");
		String ep = "https://localhost:65322/rest/oidc";
		p.setProperty("oidc.endpoint", ep);
		p.setProperty("truststore.type", "directory");
		p.setProperty("truststore.directoryLocations.1", "src/test/resources/certs/demo-ca-cert.pem");
		a.setProperties(p);
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		JSONObject req = MockOIDCServer.x.remove(0);
		System.out.println(req.toString(2));
		assertEquals("client_credentials", req.getString("grant_type"));
		assertEquals("foo", req.getString("username"));
		assertEquals("bar", req.getString("password"));
		var h = m.getHeader("Authorization");
		assertNotNull(h);
		assertEquals("Bearer some_access_token", h.getValue());
		JSONObject rt = new JSONObject(FileUtils.readFileToString(
				new File("target/oidc_refresh_token"), "UTF-8"));
		System.out.println(rt.toString(2));
		String storedRefreshToken = rt.getJSONObject(ep).getString("refresh_token");
		assertEquals("some_refresh_token", storedRefreshToken);
	}

	@Test
	public void testOIDCProxyAuthN() throws Exception {
		var a = new OIDCAgentAuthN();
		a.setAgentProxy(new MockAP());
		var p = new Properties();
		p.setProperty("oidc-agent.account", "test");
		a.setProperties(p);
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		assertNotNull(h);
		assertEquals("Bearer some_access_token", h.getValue());
	}

	public static class MockAP extends OIDCAgentProxy {
		@Override
		public String send(String data) {
			JSONObject request = new JSONObject(data);
			assertEquals("test", request.getString("account"));
			JSONObject j = new JSONObject();
			j.put("status", "success");
			j.put("access_token", "some_access_token");
			j.put("refresh_token", "some_refresh_token");
			return j.toString();
		}
	}
}