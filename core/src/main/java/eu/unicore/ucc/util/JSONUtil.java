package eu.unicore.ucc.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.services.restclient.BaseClient;

public class JSONUtil {

	private JSONUtil(){}

	public static Map<String,String>asMap(JSONObject o){
		return BaseClient.asMap(o);
	}

	public static JSONObject asJSON(Map<String,String>map){
		return BaseClient.asJSON(map);
	}

	public static List<String>asList(JSONArray o) throws JSONException{
		List<String>result = new ArrayList<>();
		for(int i=0;i<o.length();i++) {
			result.add(String.valueOf(o.get(i)));
		}
		return result;
	}

	private static String commentPattern = "(?m)^\\s*#.*\\n";

	/**
	 * read the JSONObject, ignoring comments
	 * @param json
	 */
	public static JSONObject read(String json) throws JSONException {
		return new JSONObject(json.replace("\r\n", "\n").replaceAll(commentPattern, "\n"));
	}

	public static String extractResourceID(String url) throws IOException {
		return new File(new URL(url).getPath()).getName();
	}
}
