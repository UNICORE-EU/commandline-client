package eu.unicore.ucc.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.client.core.SiteClient;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.lookup.Connector;
import eu.unicore.ucc.runner.WeightedSelection;
import eu.unicore.ucc.util.EmbeddedTestBase;
import eu.unicore.util.httpclient.IClientConfiguration;

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

	private void createInFiles()throws IOException{
		String job="{ApplicationName: Date}";
		int n=4;
		for(int i=0;i<n;i++){
			try(var fos = new FileOutputStream("target/test/batchtest/in/job"+i+".u")){
				fos.write(job.getBytes());
			}
		}
	}

	@Test
	public void testBatch_slow()throws Exception{
		FileUtils.deleteQuietly(new File("target/test/batchtest"));
		File dir=new File("target/test/batchtest/out");
		dir.mkdirs();
		dir=new File("target/test/batchtest/in");
		dir.mkdirs();
		createInFiles1();
		connect();
		String[]args=new String[]{"batch",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-i", "target/test/batchtest/in",
				"-o", "target/test/batchtest/out",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	private void createInFiles1()throws IOException{
		String job = "{Executable: '/bin/sleep', Arguments:['2']}";
		int n=4;
		for(int i=0;i<n;i++){
			try(var fos = new FileOutputStream("target/test/batchtest/in/job"+i+".u")){
				fos.write(job.getBytes());
			}
		}
	}

	@Test
	public void testWeightedSelection() throws Exception{
		connect();
		String url = Connector._last_TSS;
		IClientConfiguration sec = uas.getKernel().getClientConfiguration();
		SiteClient sc = new SiteClient(url, sec, new UsernamePassword("demouser", "test123"));
		WeightedSelection ws = new WeightedSelection(new File("src/test/resources/conf/testweights.properties"));
		var sites=new ArrayList<SiteClient>();
		sites.add(sc);
		var selected = ws.select(sites);
		assertEquals(sc, selected);
	}

}
