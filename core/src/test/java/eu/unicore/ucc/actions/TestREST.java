package eu.unicore.ucc.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestREST extends EmbeddedTestBase {

	static String url;

	@BeforeAll
	public static void setup() throws Exception {
		connect();
		var args=new String[]{"run", "-a",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/empty.u",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		url = Run.getLastJobAddress();
	}

	@Test
	public void testError() throws Exception {
		var args = new String[]{"rest", "-i",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(1),UCC.exitCode);
		args = new String[]{"rest", "-i",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"foo", "http://bar"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(1),UCC.exitCode);
	}
	
	@Test
	public void testGET() throws Exception {
		var args = new String[]{"rest", "get", "-i",
				"-c", "src/test/resources/conf/userprefs.embedded",
				url
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testGET2() throws Exception {
		var args = new String[]{"rest", "get",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--accept", "text/plain",
				"https://localhost:65322/rest/core/certificate"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testPUT() throws Exception {
		var args = new String[]{"rest", "put",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"{'tags':['foo', 'bar']}",
				url
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args = new String[]{"rest", "put",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"@src/test/resources/tags.json",
				url
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args = new String[]{"rest", "get",
				"-c", "src/test/resources/conf/userprefs.embedded",
				url+"?fields=tags"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	@Test
	public void testPOST() throws Exception {
		var args = new String[]{"rest", "post",
				"-c", "src/test/resources/conf/userprefs.embedded", "{}",
				url+"/actions/abort",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testDELETE() throws Exception {
		runDate();
		var args=new String[]{"rest", "del",
				"-c", "src/test/resources/conf/userprefs.embedded",
				Run.getLastJobAddress(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

}
