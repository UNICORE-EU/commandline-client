package eu.unicore.ucc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
		for(AuthenticationProvider p: UCC.authNMethods.values()) {
			args=new String []{"help-auth", p.getName()};
			UCC.main(args);
		}
	}

	@Test
	public void test_LoadAuthNMethods(){
		UCC.unitTesting = true;
		assertNotNull(UCC.getAuthNMethod("X509"));
		assertNotNull(UCC.getAuthNMethod("x509"));
	}

}
