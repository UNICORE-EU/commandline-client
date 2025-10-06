package eu.unicore.ucc.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class TestPropertyFilter {

	@Test
	public void testMatcher() {
		Matcher m1 = PropertyFilter.filterPattern.matcher("status!=READY");
		assertTrue(m1.matches());
		assertEquals("status", m1.group(1));
		assertEquals("!=", m1.group(2));
		assertEquals("READY", m1.group(3));

		Matcher m2 = PropertyFilter.filterPattern.matcher("status=SUCCESSFUL");
		assertTrue(m2.matches());
		assertEquals("status", m2.group(1));
		assertEquals("=", m2.group(2));
		assertEquals("SUCCESSFUL", m2.group(3));

		Matcher m3 = PropertyFilter.filterPattern.matcher("somestring");
		assertFalse(m3.matches());
	}
	
	@Test
	public void testFilter1() throws Exception {
		var pf1 = PropertyFilter.create("status==READY");
		assertFalse(pf1.accept(new JSONObject()));
		assertFalse(pf1.accept(new JSONObject("{'status': 'OK'}")));
		assertTrue(pf1.accept(new JSONObject("{'status': 'READY'}")));

		pf1 = PropertyFilter.create("status!=READY");
		assertTrue(pf1.accept(new JSONObject("{'status': 'OK'}")));
		assertFalse(pf1.accept(new JSONObject("{'status': 'READY'}")));

		pf1 = PropertyFilter.create("status~=SUCCESS");
		assertTrue(pf1.accept(new JSONObject("{'status': 'SUCCESSFUL'}")));
		assertFalse(pf1.accept(new JSONObject("{'status': 'ERROR'}")));

		pf1 = PropertyFilter.create("status!~SUCCESS");
		assertFalse(pf1.accept(new JSONObject("{'status': 'SUCCESSFUL'}")));
		assertTrue(pf1.accept(new JSONObject("{'status': 'ERROR'}")));
		
		pf1 = PropertyFilter.create("code<2");
		assertFalse(pf1.accept(new JSONObject("{'code': '3'}")));
		assertTrue(pf1.accept(new JSONObject("{'code': '1'}")));

		pf1 = PropertyFilter.create("code>2");
		assertTrue(pf1.accept(new JSONObject("{'code': '3'}")));
		assertFalse(pf1.accept(new JSONObject("{'code': '1'}")));
	}
	
	@Test
	public void testFilter2() throws Exception {
		var pf1 = PropertyFilter.create("status", "eq", "READY");
		assertFalse(pf1.accept(new JSONObject()));
		assertFalse(pf1.accept(new JSONObject("{'status': 'OK'}")));
		assertTrue(pf1.accept(new JSONObject("{'status': 'READY'}")));
	}
}
