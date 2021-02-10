package de.fzj.unicore.ucc.workflow;

import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.helpers.DefaultMessageWriter;
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
			Assert.assertEquals(Integer.valueOf(0),UCC.exitCode);
		}
	}
	
	@Test
	public void testFindBrokerImpl(){
		UCC.unitTesting=true;
		Broker b = UCC.getBroker("Local", new DefaultMessageWriter());
		Assert.assertNotNull(b);
		Assert.assertTrue(b instanceof TargetSystemFinder);
		
	}
}
