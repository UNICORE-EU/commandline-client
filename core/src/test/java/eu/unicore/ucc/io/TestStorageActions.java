package eu.unicore.ucc.io;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.unicore.ucc.Constants;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.util.EmbeddedTestBase;

/**
 * Functional tests for the UCC actions. 
 * These run against an embedded UNICORE instance.
 */
public class TestStorageActions extends EmbeddedTestBase {

	@Test
	public void test_CreateStorage() {
		
		UCC.main(new String[]{"create-storage","-v", "-c", "src/test/resources/conf/userprefs.embedded"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		UCC.main(new String[]{"create-storage","-v", "-c", "src/test/resources/conf/userprefs.embedded",
				"--lifetime", "14",
				"-f", "https://localhost:65322/rest/core/storagefactories/default_storage_factory"
		});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		
		UCC.main(new String[]{"list-storages","-l", "-v", "-c", "src/test/resources/conf/userprefs.embedded"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		
		String[] args=new String[]{"create-storage", "-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-t", "DEFAULT", "testkey=testvalue"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		// wrong factory URL
		args=new String[]{"create-storage", "-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-f", "https://localhost:65322/services/StorageFactory?res=no_such_factory",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(Constants.ERROR),UCC.exitCode);
		
		// wrong site name
		args=new String[]{"create-storage", "-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-s", "NO_SUCH_SITE",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(Constants.ERROR),UCC.exitCode);
		
		// wrong type
		args=new String[]{"create-storage", "-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-t", "NO_SUCH_TYPE",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(Constants.ERROR),UCC.exitCode);
		
	}

	@Test
	public void test_Mkdir_RM(){
		connect();

		String storage=createNewUspace();

		String dir="/test"+System.currentTimeMillis();
		String[] args=new String[]{"mkdir", "-v", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				storage+"/files"+dir,
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"rm", "-v", "-q",
				"-c", "src/test/resources/conf/userprefs.embedded",
				storage+"/files"+dir,
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Stat()throws Exception{
		connect();
		String storage=createNewUspace();

		String[] args=new String[]{"stat",
				"-c", "src/test/resources/conf/userprefs.embedded",
				storage+"/files/stdout",

		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@Test
	public void test_Umask()throws Exception{
		connect();
		String storage=createNewUspace();
		String[] args=new String[]{"umask",
				"-c", "src/test/resources/conf/userprefs.embedded",
				storage,
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"umask",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-s", "777",
				storage,
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"umask",
				"-c", "src/test/resources/conf/userprefs.embedded",
				storage,
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
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
