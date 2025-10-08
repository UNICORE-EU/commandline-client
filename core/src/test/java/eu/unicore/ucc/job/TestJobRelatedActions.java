package eu.unicore.ucc.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.info.ListActionBase;
import eu.unicore.ucc.actions.job.Allocate;
import eu.unicore.ucc.actions.job.CreateTSS;
import eu.unicore.ucc.actions.job.JobStatus;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.util.EmbeddedTestBase;

/**
 * Functional tests for the UCC actions. 
 * These run against an embedded UNICORE instance.
 */
public class TestJobRelatedActions extends EmbeddedTestBase {

	@BeforeAll
	public static void setup() {
		connect();
	}

	@Test
	public void test_Run(){
		runDate();
	}

	@Test
	public void test_RunMulti(){
		String job="src/test/resources/jobs/date.u";
		String[] args=new String[]{"run",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-a", "-J",
				job,job,job
		};		
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Run_JobFromStdin()throws IOException{
		InputStream in=System.in;
		try(FileInputStream fis=new FileInputStream("src/test/resources/jobs/date.u")){
			System.setIn(fis);
			String[] args = new String[]{"run","-v",
					"-c", "src/test/resources/conf/userprefs.embedded",
			};		
			UCC.main(args);
			assertEquals(Integer.valueOf(0),UCC.exitCode);
			System.setIn(in);
		}
	}

	@Test
	public void test_Run_JobWithUploads(){
		run("src/test/resources/jobs/date-with-uploads.u", true);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Run_With_UNICORE_Staging() throws Exception {
		File testData = new File("target/data/file1");
		FileUtils.writeByteArrayToFile(testData, "test123".getBytes());
		run("src/test/resources/jobs/date-with-u6-staging.u", true);
	}

	@Test
	public void test_Run_WithErrorInStageOut(){
		run("src/test/resources/jobs/date-with-error-in-staging.u", true);
	}

	@Test
	public void test_Run_JobWithWildcardExports(){
		run("src/test/resources/jobs/date-with-wildcard-exports.u", false);
	}

	@Test
	public void test_Run_and_ListJobs(){
		runDate();
		String[] args=new String[]{"list-jobs", "-v", "-l",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertTrue(ListActionBase.getLastNumberOfResults()>0);
	}

	@Test
	public void test_Run_Async(){
		String[] args=new String[]{"run", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/date.u",
				"-a"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		String id1 = Run.getLastJobAddress();

		args=new String[]{"run", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/date.u",
				"-a"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		String id2=Run.getLastJobAddress();
		int c=0;
		do{
			args=new String[]{"job-status",
					"-c", "src/test/resources/conf/userprefs.embedded",
					"--all",
					id1,id2,
			};
			UCC.main(args);
			assertEquals(Integer.valueOf(0),UCC.exitCode);
			c++;
			try{
				Thread.sleep(2000);
			}catch(InterruptedException i){};

		}while(c<5 && !JobStatus.allSuccessful);


		args=new String[]{"get-output", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				id1, id2
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Run_Async_MissingLocalFile()throws IOException{
		File tmp=new File("target/temp-file-for-upload");
		FileUtils.writeStringToFile(tmp, "test123", "UTF-8");

		String[] args=new String[]{"run", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/date-with-uploads-tmpfile.u",
				"-a"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		//remove local file, should not cause problems in get-status and get-outcome
		boolean delete=tmp.delete();
		if(!delete)System.out.println("Warning, did not delete file");
		String id1=Run.getLastJobAddress();	
		int c=0;
		do{
			args=new String[]{"job-status",
					"-c", "src/test/resources/conf/userprefs.embedded",
					"--long",
					id1
			};
			UCC.main(args);
			assertEquals(Integer.valueOf(0),UCC.exitCode);
			c++;
			try{
				Thread.sleep(2000);
			}catch(InterruptedException i){};

		}while(c<5 && !JobStatus.allSuccessful);

		args=new String[]{"get-output", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				id1
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Run_and_Abort(){
		String[] args=new String[]{"run", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/sleep.u",
				"-a"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		args=new String[]{"job-abort", "-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				Run.getLastJobAddress(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Run_and_Restart(){
		String[] args=new String[]{"run", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/date.u",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"job-restart", 
				"-v", "-c", "src/test/resources/conf/userprefs.embedded",
				Run.getLastJobAddress(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Run_Tags(){
		String[] args=new String[]{"run", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/date.u",
				"-T", "foo,bar,test123"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		args=new String[]{"list-jobs", "-v", "--tags", "test123",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Run_PrintSampleJob(){
		String[] args=new String[]{"run", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-H"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Exec(){
		String[] args=new String[]{"exec", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"/bin/date",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Exec_with_fake_allocation(){
		String[] args=new String[]{"exec", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				// this will serve as a fake allocation we can submit into
				"--allocation", "https://localhost:65322/rest/core/jobs",
				"/bin/date",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Allocate(){
		String[] args=new String[]{"allocate", "-v", "--dry-run",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"Nodes=2", "Runtime=1h", "Queue=dev"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertEquals("2", Allocate.lastParams.get("Nodes"));
		assertEquals("1h", Allocate.lastParams.get("Runtime"));
		assertEquals("dev", Allocate.lastParams.get("Queue"));
	}

	@Test
	public void test_CreateTSS() throws Exception {
		String[] args=new String[]{"create-tss", "-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-l", "2",
				//extra parameters
				"testkey=testvalue"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		String lastTSS = CreateTSS.getLastTargetSystemAddress();
		assertNotNull(lastTSS);
	}

}
