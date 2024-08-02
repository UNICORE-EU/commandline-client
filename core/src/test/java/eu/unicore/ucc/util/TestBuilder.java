package eu.unicore.ucc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;

public class TestBuilder{

	@Test
	public void testLocalImportsExports() throws Exception{
		File jobFile = new File("src/test/resources/jobs/date-with-uploads.u");
		UCCBuilder bob = new UCCBuilder(jobFile,null,null);
		assertNotNull(bob);
		assertEquals(2,bob.getImports().size());
	}
	
}
