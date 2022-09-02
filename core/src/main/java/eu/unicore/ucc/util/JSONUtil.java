package eu.unicore.ucc.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.services.rest.client.BaseClient;


/**
 * JSON utilities to work around some small issues with the JSON.org parser 
 * 
 * @author schuller
 */
public class JSONUtil {
	
	private JSONUtil(){}
	
	/**
	 * get the requested value
	 * @param obj - the json object
	 * @param key - the key
	 * @param defaultValue - the default value
	 */
	public static String getString(JSONObject obj, String key, String defaultValue){
		try{
			return obj.getString(key);
		}
		catch(JSONException je){
			return defaultValue;
		}
	}
	
	/**
	 * get the requested value or <code>null</code> if it does not exist in the json
	 * @param obj
	 * @param key
	 */
	public static String getString(JSONObject obj, String key){
		return getString(obj, key, null);
	}

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
	
	public static List<String> lines(String json) throws IOException {
		return IOUtils.readLines(new StringReader(json.replace("\r\n", "\n").replace("\r","\n")));
	}
	
	public static String extractResourceID(String url) throws IOException {
		return new File(new URL(url).getPath()).getName();
	}
}

