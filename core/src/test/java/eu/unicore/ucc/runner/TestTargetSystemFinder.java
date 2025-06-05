package eu.unicore.ucc.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
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
		UCCBuilder b = new UCCBuilder(j.toString(), r, conf);
		b.getMessageWriter().setVerbose(true);
		Collection<Endpoint>candidates = finder.listCandidates(r, conf, b);
		assertEquals(0, candidates.size());

		j.put("ApplicationName", "Date");
		j.put("blacklist", "localhost");
		b = new UCCBuilder(j.toString(), r, conf);
		b.getMessageWriter().setVerbose(true);
		candidates = finder.listCandidates(r, conf, b);
		assertEquals(0, candidates.size());
		
		j = new JSONObject();
		j.put("ApplicationName", "Date");
		b = new UCCBuilder(j.toString(), r, conf);
		b.getMessageWriter().setVerbose(true);
		candidates = finder.listCandidates(r, conf, b);
		assertEquals(1, candidates.size());
		
	}
}
