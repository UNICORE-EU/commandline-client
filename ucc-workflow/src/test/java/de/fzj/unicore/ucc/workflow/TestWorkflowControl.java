package de.fzj.unicore.ucc.workflow;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import de.fzj.unicore.ucc.UCC;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowFactoryClient;

public class TestWorkflowControl extends EmbeddedTestBase {

	@Test @Ignore
	public void testAbortWorkflow() throws Exception {
		connect();
		String[] args=new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"--wait",
				"src/test/resources/workflows/date1.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		String url = SubmitWorkflow.lastAddress;
		args=new String[]{"workflow-control","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"abort", url
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	@Test
	public void testResumeWorkflow() throws Exception {
		connect();
		String[] args=new String[]{"workflow-submit","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/workflows/hold.json"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		String url = SubmitWorkflow.lastAddress;
		WorkflowClient wf = new WorkflowClient(new Endpoint(url), uas.getKernel().getClientConfiguration(), null);
		wf.setUpdateInterval(-1);
		int c=0;
		while(WorkflowClient.Status.HELD!=wf.getStatus() && c<10) {
			Thread.sleep(1000);
			c++;
		}
		System.out.println(wf.getProperties().toString(2));
		assertEquals("123", wf.getProperties().getJSONObject("parameters").getString("COUNTER"));
		args=new String[]{"workflow-control","-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"resume", 
				url,
				"COUNTER=999"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		c=0;
		while(!wf.isFinished() && c<20) {
			Thread.sleep(1000);
			c++;
		}
		System.out.println(wf.getProperties().toString(2));
		assertEquals("999", wf.getProperties().getJSONObject("parameters").getString("COUNTER"));
	}
}
