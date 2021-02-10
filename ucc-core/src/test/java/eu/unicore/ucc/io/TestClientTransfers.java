package eu.unicore.ucc.io;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.util.EmbeddedTestBase;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.actions.ActionBase;

public class TestClientTransfers extends EmbeddedTestBase {

	@Test
	public void testDownloader() throws Exception {
		String[] args=new String[]{"list-storages",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		ActionBase b = (ActionBase)UCC.initCommand(args, false);
		b.initConfigurationProvider();
		b.setVerbose(true);
		UCCConfigurationProvider ucp = b.getConfigurationProvider();
		
		String url = "https://localhost:65322/rest/core/storages/WORK";
		File testData = new File("target/data/test1.dat");
		FileUtils.writeByteArrayToFile(testData, "test123".getBytes());
		
		StorageClient sc = new StorageClient(new Endpoint(url), 
				ucp.getClientConfiguration(url), ucp.getRESTAuthN());
		System.out.println(sc.getProperties().toString(2));
		
		FileDownloader fd = new FileDownloader("test1.dat", "target/data/download1.dat", FileTransferBase.Mode.NORMAL);
		fd.perform(sc, b);
		checkFilesOK(testData, new File("target/data/download1.dat"));
	}

	@Test
	public void testUploader() throws Exception {
		String[] args=new String[]{"list-storages",
				"-c", "src/test/resources/conf/userprefs.embedded",
		};
		ActionBase b = (ActionBase)UCC.initCommand(args, false);
		b.initConfigurationProvider();
		b.setVerbose(true);
		UCCConfigurationProvider ucp = b.getConfigurationProvider();
		
		String url = "https://localhost:65322/rest/core/storages/WORK";
		File testData = new File("target/data/test1.dat");
		FileUtils.writeByteArrayToFile(testData, "test123".getBytes());
		
		StorageClient sc = new StorageClient(new Endpoint(url), 
				ucp.getClientConfiguration(url), ucp.getRESTAuthN());
		System.out.println(sc.getProperties().toString(2));
		
		FileUploader fd = new FileUploader("target/data/test1.dat", "/upload1.dat", FileTransferBase.Mode.NORMAL);
		fd.perform(sc, b);
		checkFilesOK(testData, new File("target/data/upload1.dat"));
	}
}
