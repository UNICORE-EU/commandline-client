package eu.unicore.ucc.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.uas.json.ApplicationRequirement;

public class TestRequirements {
	
	@Test
	public void testApplicationRequirement() throws Exception {
		JSONObject tss=getTSSProps();
		ApplicationRequirement req=new ApplicationRequirement("Date",null);
		assertTrue(req.isFulfilled(tss));
		assertFalse(req.getDescription().contains(" v"));

		req=new ApplicationRequirement("Date","1.0");
		assertTrue(req.isFulfilled(tss));
		assertTrue(req.getDescription().contains(" v"));

		req=new ApplicationRequirement("Date","2.0");
		assertTrue(req.isFulfilled(tss));

		// TODO
		// ApplicationRequirement req2=new ApplicationRequirement("Date","2.0");
		// assertEquals(req,req2);
	}
	
	private JSONObject getTSSProps() throws JSONException {
		JSONObject tss = new JSONObject();
		JSONArray apps = new JSONArray();
		apps.put("Date---v2.0");
		tss.put("applications", apps);
		return tss;
	}
}
