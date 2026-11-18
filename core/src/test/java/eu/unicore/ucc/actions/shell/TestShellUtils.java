package eu.unicore.ucc.actions.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TestShellUtils {

	@Test
	public void testShellArgsParsing() throws Exception {
		String cmd = "this is a test";
		List<String> res = Arrays.asList(Shell.tokenizeCmdline(cmd));
		assertEquals(4, res.size());	
		cmd = "this \"is a\" test";
		res = Arrays.asList(Shell.tokenizeCmdline(cmd));
		assertEquals(3, res.size());
		assertEquals("is a", res.get(1));
		cmd = "this \"is a test\"";
		res = Arrays.asList(Shell.tokenizeCmdline(cmd));
		assertEquals(2, res.size());
		assertEquals("is a test", res.get(1));
		cmd = "set a=b";
		res = Arrays.asList(Shell.tokenizeCmdline(cmd));
		assertEquals(2, res.size());
		assertEquals("set", res.get(0));
		assertEquals("a=b", res.get(1));		
		cmd = "set a=\"this is value b\"";
		res = Arrays.asList(Shell.tokenizeCmdline(cmd));
		assertEquals(2, res.size());
		assertEquals("set", res.get(0));
		assertEquals("a=this is value b", res.get(1));
		cmd = "set dn=\"CN=\\\"Some Guy\\\"\"";
		res = Arrays.asList(Shell.tokenizeCmdline(cmd));
		assertEquals(2, res.size());
		assertEquals("set", res.get(0));
		assertEquals("dn=CN=\"Some Guy\"", res.get(1));
	}

}
