package eu.unicore.ucc.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestBatchEmbedded extends EmbeddedTestBase {


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
