package eu.unicore.ucc.authn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.security.canl.PasswordCallback;
import eu.unicore.security.wsutil.client.authn.UsernameCallback;
import eu.unicore.ucc.UCC;

public class TestCallbackUtils {

	@BeforeAll
	public static void setup() {
		UCC.unitTesting = true;
	}

	@AfterAll
	public static void teardown() {
		UCC.unitTesting = false;
	}

	@Test
	public void testGetPassword() throws Exception {
		String r = new String(CallbackUtils.getPasswordFromUserCmd("test", "test"));
		assertEquals("test123",r);
	}

	@Test
	public void testPasswordCallback() throws Exception {
		PasswordCallback pc = CallbackUtils.getPasswordCallback();
		assertFalse(pc.askForSeparateKeyPassword());
		assertFalse(pc.ignoreProperties());
		assertEquals("test123", new String(pc.getPassword("test", "test")));
	}

	@Test
	public void testGetUsername() throws Exception {
		assertEquals("test123", CallbackUtils.getUsernameFromUser("test"));
	}

	@Test
	public void testUsernameCallback() throws Exception {
		UsernameCallback uc = CallbackUtils.getUsernameCallback();
		assertEquals("test123", uc.getUsername());
	}

}
