package eu.unicore.ucc.job;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.util.EmbeddedTestBase;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class TestBatchEmbedded extends EmbeddedTestBase {


	@FunctionalTest(id="testBatch", description="Tests the 'batch' command")
	@Test
	public void testBatch()throws Exception{
		FileUtils.deleteQuietly(new File("target/test/batchtest"));
		File dir=new File("target/test/batchtest/out");
		dir.mkdirs();
		dir=new File("target/test/batchtest/in");
		dir.mkdirs();
		createInFiles();
		
		connect();
		
		String[]args=new String[]{"batch",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-i", "target/test/batchtest/in",
				"-o", "target/test/batchtest/out",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	int n=4;
	
	private void createInFiles()throws IOException{
		String job="{ApplicationName: Date}";
		for(int i=0;i<n;i++){
			FileOutputStream fos=new FileOutputStream("target/test/batchtest/in/job"+i+".u");
			fos.write(job.getBytes());
			fos.close();
		}
	}
}
