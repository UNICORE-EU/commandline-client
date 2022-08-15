package de.fzj.unicore.ucc.workflow;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.fzj.unicore.ucc.UCC;
import eu.unicore.client.Endpoint;
import eu.unicore.workflow.WorkflowFactoryClient;

public class TestWorkflowControl extends EmbeddedTestBase {

	@Test
	public void testAbortWorkflow() throws Exception {
		connect();
		String[] args=new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--wait",
				"src/test/resources/workflows/date1.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		WorkflowFactoryClient wfc = new WorkflowFactoryClient(new Endpoint("https://localhost:65322/rest/workflows"), 
				uas.getKernel().getClientConfiguration(), null);
		String u = wfc.getWorkflowList().getUrls(0, 1).get(0);
		args=new String[]{"workflow-control","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"abort", u
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
}
