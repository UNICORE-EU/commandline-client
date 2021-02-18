package de.fzj.unicore.ucc.workflow;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.fzj.unicore.ucc.UCC;

public class TestSubmitWorkflow extends EmbeddedTestBase {

	@Test
	public void testSystemInfo() {
		File sessions=new File("target","ucc-session-ids");
		FileUtils.deleteQuietly(sessions);

		connect();
		String[] args=new String[]{"system-info","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		args=new String[]{"system-info", "-l",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testRunWorkflow() {
		File sessions=new File("target","ucc-session-ids");
		FileUtils.deleteQuietly(sessions);

		connect();
		String[] args=new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--wait",
				"src/test/resources/workflows/date1.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

}
