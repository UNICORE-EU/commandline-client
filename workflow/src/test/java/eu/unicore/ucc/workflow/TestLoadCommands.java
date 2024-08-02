package eu.unicore.ucc.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.runner.Broker;
import eu.unicore.ucc.runner.TargetSystemFinder;

public class TestLoadCommands {

	private String[]cmds=new String[]{"workflow-submit", "list-workflows", "workflow-control"};
	
	@Test
	public void testLoadCommands(){
		UCC.unitTesting=true;
		for(String cmd: cmds){
			String[] args=new String[]{cmd, "-h"
			};
			UCC.main(args);
			assertEquals(Integer.valueOf(0),UCC.exitCode);
		}
	}
	
	@Test
	public void testFindBrokerImpl(){
		UCC.unitTesting=true;
		Broker b = UCC.getBroker("Local");
		assertNotNull(b);
		assertTrue(b instanceof TargetSystemFinder);
		
	}
}
