package eu.unicore.ucc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import eu.unicore.uas.UAS;
import eu.unicore.ucc.Command;
import eu.unicore.ucc.UCC;

/**
 * starts an embedded UNICORE/X server for testing
 * 
 * @author schuller
 */
public abstract class EmbeddedTestBase {

	public boolean isVerbose(){return true;}

	protected static UAS uas;

	public static synchronized void setUp(String config)throws Exception{
		System.out.println("Starting embedded UNICORE/X server.");
		//clean data directory
		FileUtils.deleteQuietly(new File("target","data"));
		uas=new UAS(config);
		uas.startSynchronous();
		UCC.unitTesting=true;
		Command.quitAfterPrintingUsage=false;
	}

	@BeforeEach
	public synchronized void doPreClean()throws Exception{
		expected.clear();
		gotExpectedOutput = false;
		prefsFile = "src/test/resources/conf/userprefs.embedded";
	}

	@BeforeAll
	public static synchronized void setUp()throws Exception{
		setUp("src/test/resources/uas/uas.config");
	}

	@AfterAll
	public static synchronized void tearDown()throws Exception{
		if(uas==null)return;
		System.out.println("Stopping embedded UNICORE/X server.");
		try{
			uas.getKernel().shutdown();
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}

	public void verbose(String message){
		System.out.println("[ucc testing] "+message);
	}

	public void message(String message){
		System.out.println("[ucc testing] "+message);
		for(String s: expected){
			if(message.contains(s)){
				gotExpectedOutput = true;
				break;
			}
		}
	}

	public void error(String message, Throwable cause){
		System.err.println("[ucc testing] "+message);
		if(cause!=null)cause.printStackTrace();
	}

	protected void delete(File directory)throws IOException{
		for(File f: directory.listFiles()){
			if(f.isDirectory()){
				delete(f);
			}
			else f.delete();
		}
	}

	protected String prefsFile = "src/test/resources/conf/userprefs.embedded"; 

	protected void connect(){
		UCC.main(new String[]{"connect","-v","-c",prefsFile});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	protected void listStorages(){
		UCC.main(new String[]{"list-storages","-l","-c","src/test/resources/conf/userprefs.embedded"});
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}
	
	protected void runDate(){
		run("src/test/resources/jobs/date.u",false);
	}

	protected void run(String jobFile, boolean verbose){
		run(jobFile, verbose, true);
	}
		
	protected void run(String jobFile, boolean verbose, boolean ignoreFailure){
		String[] args=new String[]{"run",
				"-c", "src/test/resources/conf/userprefs.embedded",
				jobFile
		};
		if(verbose){
			List<String>a=new ArrayList<>();
			a.addAll(Arrays.asList(args));
			a.add("-v");
			args=a.toArray(new String[a.size()]);
		}
		UCC.main(args);
		if(!ignoreFailure)assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	protected void upload(String file, String target, boolean verbose){
		String[] args=new String[]{"cp",
				"-c", "src/test/resources/conf/userprefs.embedded",
				file, 
				target,
		};

		if(verbose){
			List<String>a=new ArrayList<String>();
			a.addAll(Arrays.asList(args));
			a.add("-v");
			args=a.toArray(new String[a.size()]);
		}
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
	}

	//check MD5 hash and fail() if no match
	public void checkFilesOK(File expected, File actual){
		try{
			String expectedDigest=computeDigest(expected);
			String actualDigest=computeDigest(actual);
			assertEquals(expectedDigest,actualDigest);
		}
		catch(Exception ex){
			fail();
		}
	}

	private String computeDigest(File file)throws Exception{
		MessageDigest md=MessageDigest.getInstance("MD5");
		byte[] buf=new byte[1024];
		try(FileInputStream fis=new FileInputStream(file)) {
			while(true){
				int len=fis.read(buf);
				if(len<0)break;
				md.update(buf,0,len);
			}
		}
		byte[]d1=md.digest();
		return new String(Base64.encode(d1));
	}

	final Set<String>expected = new HashSet<>();
	
	public boolean gotExpectedOutput = false;
	
	public void expect(String message){
		expected.add(message);
	}
}
