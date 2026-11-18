package eu.unicore.ucc.authn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.hamcrest.Matchers.nu;

import org.hamcrest.Matchers;
import org.jline.reader.LineReader;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.junit5.JUnit5Mockery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import eu.unicore.security.canl.PasswordCallback;
import eu.unicore.security.wsutil.client.authn.UsernameCallback;
import eu.unicore.ucc.UCC;

public class TestCallbackUtils {

	@AfterEach
	public void teardown() {
		UCC.setLineReader(null);
	}

	@Test
	public void testGetPassword() throws Exception {
		var context = setup();
		String r = new String(CallbackUtils.getPasswordFromUserCmd("test", "test"));
		assertEquals("test123",r);
		context.assertIsSatisfied();
	}

	@Test
	public void testPasswordCallback() throws Exception {
		var context = setup();
		PasswordCallback pc = CallbackUtils.getPasswordCallback();
		assertFalse(pc.askForSeparateKeyPassword());
		assertFalse(pc.ignoreProperties());
		assertEquals("test123", new String(pc.getPassword("test", "test")));
		context.assertIsSatisfied();
	}

	@Test
	public void testGetUsername() throws Exception {
		var context = setup();
		assertEquals("test123", CallbackUtils.getUsernameFromUser("test"));
		context.assertIsSatisfied();
	}

	@Test
	public void testUsernameCallback() throws Exception {
		var context = setup();
		UsernameCallback uc = CallbackUtils.getUsernameCallback();
		assertEquals("test123", uc.getUsername());
		context.assertIsSatisfied();
	}

	private Mockery setup() {
		Mockery context = new JUnit5Mockery();
		LineReader lr = context.mock(LineReader.class);
		context.checking(new Expectations() {{
			oneOf(lr).readLine(with(Matchers.anyOf(any(Character.class), Matchers.nullValue())));
			will(returnValue("test123"));
		}});
		UCC.setLineReader(lr);
		return context;
	}
}
