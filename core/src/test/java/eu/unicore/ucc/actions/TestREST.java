package eu.unicore.ucc.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestREST extends EmbeddedTestBase {
	
	@Override
	protected void connect(){
		UCC.main(new String[]{"connect",
				"-c", "src/test/resources/conf/userprefs.embedded"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testREST() throws Exception {
		connect();
		
		String[] args=new String[]{"run", "-a",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/empty.u",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		
		args=new String[]{"rest", "get", "-i",
				"-c", "src/test/resources/conf/userprefs.embedded",
				Run.getLastJobAddress(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"rest", "put",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"{'tags':['foo', 'bar']}",
				Run.getLastJobAddress(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"rest", "get",
				"-c", "src/test/resources/conf/userprefs.embedded",
				Run.getLastJobAddress()+"?fields=tags",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"rest", "post",
				"-c", "src/test/resources/conf/userprefs.embedded", "{}",
				Run.getLastJobAddress()+"/actions/abort",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		Thread.sleep(2);

		args=new String[]{"rest", "get",
				"-c", "src/test/resources/conf/userprefs.embedded",
				Run.getLastJobAddress()+"?fields=status",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"rest", "del",
				"-c", "src/test/resources/conf/userprefs.embedded",
				Run.getLastJobAddress(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

}
