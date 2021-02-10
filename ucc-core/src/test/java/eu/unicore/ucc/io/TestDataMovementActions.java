package eu.unicore.ucc.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.fzj.unicore.uas.util.UnitParser;
import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.util.EmbeddedTestBase;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.bugsreporter.annotation.RegressionTest;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.ucc.actions.data.CP;
import eu.unicore.ucc.actions.data.LS;
import eu.unicore.ucc.actions.job.Run;

public class TestDataMovementActions extends EmbeddedTestBase {

	@Test
	public void test_PutFile_GetFile_WithWildcards()throws IOException{
		connect();
		String storage = createStorage();

		String[] args=new String[]{"cp", "src/test/resources/testfiles/file*",
				"BFT:"+storage+"/files/",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"ls","-l","-H", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		File tmpfolder=new File("target/testdata","ucc-"+System.currentTimeMillis());
		if(!tmpfolder.exists()){
			assertTrue("can't create test directory", tmpfolder.mkdirs());
		}
		args=new String[]{"cp", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/file*",
				tmpfolder.getPath(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		//check that we got all the files
		assertTrue(new File(tmpfolder,"file1").exists());
		assertTrue(new File(tmpfolder,"file2").exists());

	}

	@FunctionalTest(id="testPutGetFileErrorHandling", description="Tests error handling of up/download.")
	@Test
	public void test_Error_PutFile_GetFile()throws IOException{
		connect();
		String storage=createStorage();

		String[] args=new String[]{"cp", "target/NOSUCHFILE",
				"BFT:"+storage+"/files/",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v"
		};
		UCC.main(args);
		assertFalse(Integer.valueOf(0).equals(UCC.exitCode));

		//upload a test file
		args=new String[]{"cp", "src/test/resources/testfiles/file1",
				"BFT:"+storage+"/files/",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertTrue(Integer.valueOf(0).equals(UCC.exitCode));

		//download to a non-existing target
		String target="target/NOSUCHDIRECTORY";
		args=new String[]{"cp", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/file*",
				target,
		};
		UCC.main(args);
		assertFalse(Integer.valueOf(0).equals(UCC.exitCode));
	}

	@Test
	public void test_PutFile_LS_GetFile_Find()throws IOException{
		connect();
		String storage=createStorage();

		String[] args=new String[]{"cp", "src/test/resources/jobs/date.u",
				"BFT:"+storage+"/files/test",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"ls","-l","-H", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		File f=File.createTempFile("ucc-test", "data");
		f.deleteOnExit();

		args=new String[]{"cp", "BFT:"+storage+"/files/test",
				f.getAbsolutePath(),
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		checkFilesOK(new File("src/test/resources/jobs/date.u"), f);

	}

	@FunctionalTest(id="testGetFileRawHttp", description="Tests the get-file from a http source")
	@Test
	public void test_GetFile_RawHttpURL()throws IOException{
		connect();
		File f=new File("target","test-get-file"+System.currentTimeMillis());
		String[] args=new String[]{"cp", "https://localhost:65322/rest/core",
				f.getPath(),
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@FunctionalTest(id="testFileOperationByteRanges", description="Tests the put/get-file ops using a byte range")
	@Test
	public void test_FileOps_ByteRange()throws IOException{
		connect();
		String storage=createStorage();
		String data="0123456789abcdefghijklmnopqrstuvwxyz";
		File t=new File("target","test-upload-"+System.currentTimeMillis());
		FileUtils.writeStringToFile(t, data, "UTF-8");

		int endByte=3;
		String[] args=new String[]{"cp", t.getPath(),
				"BFT:"+storage+"/files/test",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-B", "0-"+endByte,
				"-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"ls","-l","-H", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/test",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		FileListEntry lsResult = LS.getLastLS();
		assertEquals(endByte+1, lsResult.size);

		//upload fully
		args=new String[]{"cp", t.getPath(),
				"BFT:"+storage+"/files/test2",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"ls","-l","-H", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/test",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		//get part of file
		t=new File("target","test-get-file"+System.currentTimeMillis());
		args=new String[]{"cp", "BFT:"+storage+"/files/test2",
				t.getPath(),
				"-B","1-4",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		assertEquals(4l,t.length());

		String content=FileUtils.readFileToString(t, "UTF-8");
		assertEquals("1234",content);

		//get-file using raw http
		args=new String[]{"cp", "https://localhost:65322/rest/core",
				t.getPath(),
				"-B","1-4",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}


	@FunctionalTest(id="testCopyFileStatus", description="Tests the copy-file-status commands")
	@Test
	public void test_CopyFile_and_CopyFileStatus()throws IOException{
		connect();
		String storage=createStorage();
		String storage2=createStorage();

		String[] args=new String[]{"cp", "src/test/resources/jobs/date.u",
				"BFT:"+storage+"/files/test",
				"-c", "src/test/resources/conf/userprefs.embedded",

		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/test",
				"BFT:"+storage2+"/files/test",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		File f=File.createTempFile("ucc-test", "data");
		f.deleteOnExit();

		args=new String[]{"cp", "BFT:"+storage2+"/files/test",
				f.getAbsolutePath(),
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		checkFilesOK(new File("src/test/resources/jobs/date.u"), f);

		//same in async mode
		args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/test",
				"BFT:"+storage2+"/files/test_async",
				"-a","-v"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"copy-file-status", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				CP.lastTransferAddress,
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);


		File f2=File.createTempFile("ucc-test", "data-2");
		f2.deleteOnExit();
		args=new String[]{"cp", 
				"BFT:"+storage2+"/files/test",
				f2.getAbsolutePath(),
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		checkFilesOK(new File("src/test/resources/jobs/date.u"), f2);
	}

	@FunctionalTest(id="testListFiletransfers", description="Tests the list-transfers commands")
	@Test
	public void test_ListTransfers()throws IOException{
		connect();
		String storage = createStorage();
		String storage2 = createStorage();

		String[] args=new String[]{"cp", "src/test/resources/jobs/date.u",
				storage+"/files/test",
				"-c", "src/test/resources/conf/userprefs.embedded",

		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		//copy in async mode
		args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				storage+"/files/test",
				storage2+"/files/test_async",
				"-a",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		//scheduled in async mode
		Calendar startTime=Calendar.getInstance();
		startTime.add(Calendar.DATE, 1);

		args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				storage+"/files/test",
				storage2+"/files/test_async",
				"-a","-S", UnitParser.getISO8601().format(startTime.getTime()),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);


		args=new String[]{"list-transfers", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-l", 
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@RegressionTest(url="https://sourceforge.net/tracker/?func=detail&aid=3406663&group_id=102081&atid=633902")
	@Test
	public void test_Error_CopyFile()throws IOException{
		connect();
		String storage=createStorage();
		String storage2=createStorage();

		String[] args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/NOSUCHFILE",
				"BFT:"+storage2+"/files/",
		};
		UCC.main(args);
		assertFalse(Integer.valueOf(0)==UCC.exitCode);
	}

	@FunctionalTest(id="testGetDirectory", description="Tests get-file of a directory")
	@Test
	public void test_ExportDirectory()throws IOException{
		connect();
		String storage=createStorage();
		//upload files
		String[] args=new String[]{"cp", "src/test/resources/testfiles/*",
				"BFT:"+storage+"/files/",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		//mkdir
		args=new String[]{"mkdir","BFT:"+storage+"/files/sub",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		//upload more files
		args=new String[]{"cp", "src/test/resources/testfiles/*",
				"BFT:"+storage+"/files/sub/",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		//and export the complete directory
		File localTarget=new File("target","ucc-export-"+System.currentTimeMillis());
		localTarget.mkdirs();
		args=new String[]{"cp",
				"BFT:"+storage+"/files/",
				localTarget.getPath(),
				"-R",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		//check subdir is there
		File sub=new File(localTarget,"sub");
		assertTrue(sub.exists() && sub.isDirectory());
		//and that files in subdirectory exist
		assertEquals(3, sub.listFiles().length);
	}


	@FunctionalTest(id="testPutDirectory", description="Tests put-file of a directory")
	@Test
	public void test_ImportDirectory()throws IOException{
		connect();
		String storage=createStorage();
		//create a directory with a few files
		String dirname="ucctest-"+System.currentTimeMillis();
		File tmp=new File(System.getProperty("java.io.tmpdir"), dirname);
		assertTrue("Can't create tmpdir",tmp.mkdir());
		FileUtils.writeStringToFile(new File(tmp,"test1"), "test1", "UTF-8");
		FileUtils.writeStringToFile(new File(tmp,"test2"), "test2", "UTF-8");
		File subdir=new File(tmp, "sub");
		assertTrue("Can't create tmpdir",subdir.mkdir());
		FileUtils.writeStringToFile(new File(subdir,"sub1"), "sub1", "UTF-8");

		//upload a complete directory
		String[] args=new String[]{"cp", tmp.getAbsolutePath(),
				"BFT:"+storage+"/files/",
				"-R",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		//export the complete directory
		File localTarget=new File("target","ucc-export-"+System.currentTimeMillis());
		localTarget.mkdirs();
		args=new String[]{"cp", 
				"BFT:"+storage+"/files/",
				localTarget.getPath(),
				"-R",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		//check subdir is there
		File sub=new File(localTarget,dirname+"/sub");
		assertTrue(sub.exists() && sub.isDirectory());
		//and that files in subdirectory exist
		assertTrue(sub.listFiles().length==1);
		FileUtils.deleteQuietly(tmp);
	}

	@FunctionalTest(id="testCatFile", description="Tests the 'cat' command")
	@Test
	public void test_CatFile()throws IOException{
		connect();
		String storage = createUspace();
		String[] args=new String[]{"cat",
				"BFT:"+storage+"/files/stdout",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"cat",
				"BFT:"+storage+"/files/stdout",
				"BFT:"+storage+"/files/stdout",
				"BFT:"+storage+"/files/stdout",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"cat",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(1),UCC.exitCode);

		args=new String[]{"cat",
				"BFT:"+storage+"/files/std*",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	@FunctionalTest(id="testCP", description="Tests the CP command")
	@Test
	public void test_CP()throws IOException{
		connect();
		String storage=createStorage();

		// client to server

		String[] args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded","-v",
				"src/test/resources/testfiles/file*",
				storage+"/files/",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		args=new String[]{"ls","-l","-H", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				storage
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		File tmpfolder=new File("target/testdata","ucc-"+System.currentTimeMillis());
		if(!tmpfolder.exists()){
			assertTrue("can't create test directory", tmpfolder.mkdirs());
		}
		args=new String[]{"cp", "-v",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/file*",
				tmpfolder.getPath(),
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		//check that we got all the files
		assertTrue(new File(tmpfolder,"file1").exists());
		assertTrue(new File(tmpfolder,"file2").exists());

		// server to server
		String storage2=createStorage();

		args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/file1",
				"BFT:"+storage2+"/files/test",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		// with other protocol
		args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/file1",
				"BFT:"+storage2+"/files/file_1",
				"-P", "RBYTEIO"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

		// same server
		args=new String[]{"cp", 
				"-c", "src/test/resources/conf/userprefs.embedded",
				"BFT:"+storage+"/files/file1",
				"BFT:"+storage+"/files/file_1",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);

	}

	protected String createStorage(){
		run("src/test/resources/jobs/empty.u", false);
		return Run.getLastJobDirectory();
	}

	protected String createUspace(){
		run("src/test/resources/jobs/date.u", false);
		return Run.getLastJobDirectory();
	}
	
}
