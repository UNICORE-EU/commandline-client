package eu.unicore.ucc.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.util.EchoServer;
import eu.unicore.ucc.util.EmbeddedTestBase;

public class TestOpenTunnel extends EmbeddedTestBase {

	@Test
	public void test_OpenTunnel() throws Exception {
		File sessions=new File("target","ucc-session-ids");
		FileUtils.deleteQuietly(sessions);
		connect();
		EchoServer ec = new EchoServer();
		ec.start();
		runDate();

		final int applicationPort = ec.getServerPort();
		final int localPort = applicationPort + 1;
		final String endpoint = Run.getLastJobAddress()+"/forward-port?port="+applicationPort;
		boolean stop = false;
		Thread forwarder = new Thread(()->{
			String[] args=new String[]{"open-tunnel","-v",
					"-c", "src/test/resources/conf/userprefs.embedded",
					"-L", String.valueOf(localPort),
					endpoint
			};
			UCC.main(args);
		});
		forwarder.start();
		int attempts = 0;
		while(!stop && attempts < 5) {
			Thread.sleep(1000);
			try(Socket s = new Socket("127.0.0.1", localPort);
				PrintWriter pw = new PrintWriter(s.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())))
			{
				pw.write("test123\n");
				pw.flush();
				String line = br.readLine();
				System.out.println("Echo reply *** "+line);
				stop = true;
			}catch(Exception e) {
				attempts++;
			}
		}
	}

}