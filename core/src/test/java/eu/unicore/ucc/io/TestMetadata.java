package eu.unicore.ucc.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.data.CreateStorage;
import eu.unicore.ucc.actions.data.Metadata;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.util.EmbeddedTestBase;

/**
 * Functional tests for the UCC actions. 
 * These run against an embedded UNICORE instance.
 */
public class TestMetadata extends EmbeddedTestBase {
	
	@Test
	public void test_Metadata()throws Exception{
		connect();

		String[] args=new String[]{"create-storage", "-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		
		String storage = CreateStorage.getLastStorageAddress();
		
		// create file first
		args=new String[]{"cp",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"pom.xml",
				storage+"/files/testfile",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		
		File metaFile=new File("target", "testmetadata.json");
		FileUtils.writeStringToFile(metaFile, "{foo: bar}", "UTF-8");

		args=new String[]{"metadata", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-s", storage,
				"-C", "write",
				"-f", metaFile.getPath(),
				"testfile",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"metadata",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-s", storage,
				"-C", "read",
				"testfile",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertEquals("bar", Metadata.getLastMeta().get("foo"));

		FileUtils.writeStringToFile(metaFile, "{foo: spam}", "UTF-8");
		args=new String[]{"metadata",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-s", storage,
				"-C", "update",
				"-f", metaFile.getPath(),
				"testfile",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertEquals("spam", Metadata.getLastMeta().get("foo"));

		FileUtils.writeStringToFile(metaFile, "{foo: spam}", "UTF-8");
		args=new String[]{"metadata",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-s", storage,
				"-C", "delete",
				"testfile",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"metadata",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-s", storage,
				"-C", "read",
				"testfile",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertTrue(!Metadata.getLastMeta().containsKey("foo"));

		args=new String[]{"metadata",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-s", storage,
				"-C", "start-extract",
				"testfile",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		Thread.sleep(1500);
	}
	

	protected String createNewUspace(){
		String[] args=new String[]{"run",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"src/test/resources/jobs/date.u",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		return Run.getLastJobDirectory();
	}

}
