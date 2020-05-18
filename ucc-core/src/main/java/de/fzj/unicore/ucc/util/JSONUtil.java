package de.fzj.unicore.ucc.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


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
	
	/**
	 * improve the error message by including the line number (not just the position)
	 * @param json
	 * @param ex
	 * @return error message containing the line number in the JSON
	 */
	public static String makeParseErrorMessage(String json, JSONException ex){
		String line="unknown";
		String[] parts=ex.getMessage().split("at character ");
		if(parts.length>1){
			try{
				int pos=Integer.parseInt(parts[1]);
				line=String.valueOf(getLineNumber(json, pos));
			}
			catch(Exception e){}
		}
		return "Error parsing (around line "+line+ ") "+ex.getMessage();
	}
	
	private static int getLineNumber(String s, int pos)throws IOException{
		int l=0;
		List<String>lines = lines(s);
		int c=0;
		for(String line: lines){
			if(line.matches(commentPattern))continue;
			c+=line.length();
			if(c>=pos)break;
			l++;
		}
		return l;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,String>asMap(JSONObject o){
		Map<String,String>result=new HashMap<String, String>();
		Iterator<String>i=o.keys();
		while(i.hasNext()){
			String s=i.next();
			try{
				result.put(s,o.getString(s));
			}catch(JSONException ex){}
		}
		return result;
	}
	
	public static JSONObject asJSON(Map<String,String>map){
		JSONObject o=new JSONObject();
		for(Map.Entry<String, String>entry: map.entrySet()){
			try{
				o.put(entry.getKey(), entry.getValue());
			}catch(JSONException e){}
		}
		return o;
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

