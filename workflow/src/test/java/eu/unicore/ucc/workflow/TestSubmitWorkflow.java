package eu.unicore.ucc.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.ucc.UCC;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowFactoryClient;

public class TestSubmitWorkflow extends EmbeddedTestBase {

	@Test
	public void testSystemInfo() {
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
		connect();
		String[] args = new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--wait",
				"src/test/resources/workflows/date1.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		args = new String[]{"list-workflows","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--long",
				SubmitWorkflow.lastAddress
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testLocalFiles() throws Exception {
		connect();
		String[] args=new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-u", "src/test/resources/workflows/inputs.u",
				"--wait",
				"src/test/resources/workflows/date1.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		WorkflowFactoryClient wfc = new WorkflowFactoryClient(new Endpoint("https://localhost:65322/rest/workflows"), 
				uas.getKernel().getClientConfiguration(), null);
		EnumerationClient ec = wfc.getWorkflowList();
		WorkflowClient wf = ec.createClient(ec.getUrls(0, 1).get(0), WorkflowClient.class);
		System.out.println(wf.getProperties().toString(2));
	}
	
	@Test
	public void testLocalFiles2() throws Exception {
		connect();
		String[] args=new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--tags", "test123",
				"-S", "https://localhost:65322/rest/core/storages/WORK",
				"-u", "src/test/resources/workflows/inputs.u",
				"--wait",
				"src/test/resources/workflows/date1.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		args = new String[]{"list-workflows", "-v", "--long",
				"-c", "src/test/resources/conf/userprefs.embedded",
				SubmitWorkflow.lastAddress
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testLocalFiles3() throws Exception {
		connect();
		String[] args=new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--tags", "test123",
				"-f", "https://localhost:65322/rest/core/storagefactories/default_storage_factory",
				"-u", "src/test/resources/workflows/inputs.u",
				"--wait",
				"src/test/resources/workflows/date1.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		args = new String[]{"list-workflows", "-v", "--long",
				"-c", "src/test/resources/conf/userprefs.embedded",
				SubmitWorkflow.lastAddress
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void testTemplate() throws Exception {
		File sessions=new File("target","ucc-session-ids");
		FileUtils.deleteQuietly(sessions);
		connect();
		String[] args=new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-u", "src/test/resources/workflows/template1.u",
				"--wait",
				"src/test/resources/workflows/template1.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	@Test
	public void testWorkflowList() throws Exception {
		File sessions=new File("target","ucc-session-ids");
		FileUtils.deleteQuietly(sessions);
		connect();
		String[] args=new String[]{"list-workflows","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--long",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
}
