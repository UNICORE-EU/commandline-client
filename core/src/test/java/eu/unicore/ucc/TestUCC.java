package eu.unicore.ucc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.unicore.security.wsutil.client.authn.AuthenticationProvider;

public class TestUCC {

	@Test
	public void test_PrintUsage(){
		UCC.unitTesting = true;
		UCC.printUsage(false);
		System.err.println("\n");
		UCC.printAuthNUsage();
		UCC.printAuthNUsage("X509");
	}

	@Test
	public void test_ShowHelp() throws Exception {
		UCC.unitTesting = true;
		String[]args=new String []{"-h"};
		for(Command cmd: UCC.getAllCommands()){
			try{
				cmd.init(args);
			}catch(Exception epe){}
		};
		args=new String []{"help-auth"};
		UCC.main(args);
		for(String p: UCC.authNMethods.keySet()) {
			assertNotNull(UCC.getAuthNMethod(p));
		}
		for(AuthenticationProvider p: UCC.authNMethods.values()) {
			args=new String []{"help-auth", p.getName()};
			UCC.main(args);
		}
	}

	@Test
	public void test_LoadAuthNMethods(){
		UCC.unitTesting = true;
		for(String p: UCC.authNMethods.keySet()) {
			assertNotNull(UCC.getAuthNMethod(p));
		}
		assertNotNull(UCC.getAuthNMethod("X509"));
		assertNotNull(UCC.getAuthNMethod("x509"));
		assertThrows(IllegalArgumentException.class, ()->UCC.getAuthNMethod("nosuch"));
	}

	@Test
	public void test_LoadCommand() throws Exception {
		UCC.unitTesting = true;
		assertNotNull(UCC.initCommand(new String[]{"system-info"}, false, new Properties()));
		assertNull(UCC.initCommand(new String[]{"nosuch"}, false, new Properties()));
	}
}
