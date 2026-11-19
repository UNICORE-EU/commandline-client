package eu.unicore.ucc.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.Connect;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.util.EmbeddedTestBase;
import eu.unicore.ucc.util.UCCBuilder;

public class TestTargetSystemFinder extends EmbeddedTestBase {

	@Test
	public void test_TargetSystemFinder() throws Exception {
		connect();
		Connect cmd = (Connect)UCC.lastCommand;
		IRegistryClient r = cmd.getRegistry();
		UCCConfigurationProvider conf = cmd.getConfigProvider();
		TargetSystemFinder finder = new TargetSystemFinder();
		JSONObject j = new JSONObject();

		j.put("ApplicationName", "no_such_app");
		final UCCBuilder b = new UCCBuilder(j.toString(), r, conf);
		Collection<String>candidates = finder.listCandidates(r, conf, b);
		assertEquals(0, candidates.size());
		var ex = assertThrows(Exception.class,
				()->finder.findTSSAddress(r, conf, b, new RandomSelection()));
		assertTrue(ex.getMessage().contains("No suitable endpoint"));

		j.put("ApplicationName", "Date");
		j.put("blacklist", "localhost");
		UCCBuilder b2 = new UCCBuilder(j.toString(), r, conf);
		candidates = finder.listCandidates(r, conf, b2);
		assertEquals(0, candidates.size());

		j = new JSONObject();
		j.put("ApplicationName", "Date");
		b2 = new UCCBuilder(j.toString(), r, conf);
		candidates = finder.listCandidates(r, conf, b2);
		assertEquals(1, candidates.size());
	}
}
