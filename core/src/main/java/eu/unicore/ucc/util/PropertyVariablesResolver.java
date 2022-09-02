/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.ucc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

/**
 * 
 * @author K. Benedyczak
 */
public class PropertyVariablesResolver
{
	/**
	 * Replaces all property values which contain expression ${VAR} with the value of an env. variable
	 * VAR. If there is no such variable the expression is left unchanged. The special variable
	 * UCC_CONFIG is always replaced with the absolute location of a file given as a 2nd argument.  
	 * @param properties
	 * @param propertiesFile
	 */
	public static void substituteVariables(Properties properties, File propertiesFile)
	{
		final String UCC_CONFIG = "UCC_CONFIG";
		Pattern pattern = Pattern.compile("\\$\\{[^}]*\\}");
		Map<String, String> replacements = new HashMap<String, String>(System.getenv());
		if (propertiesFile != null && propertiesFile.exists() && propertiesFile.isFile()
				 && propertiesFile.getParentFile()!=null)
			replacements.put(UCC_CONFIG, propertiesFile.getParentFile().getAbsolutePath());
		
		for (Entry<Object, Object> entry: properties.entrySet())
		{
			String value = (String) entry.getValue();
			Matcher matcher = pattern.matcher(value);
			StringBuilder sb = new StringBuilder();
			int lastEnd = 0;
			while (matcher.find())
			{
				sb.append(value.substring(lastEnd, matcher.start()));
				lastEnd = matcher.end();
				String variableExp = matcher.group();
				String variable = variableExp.substring(2, variableExp.length()-1);
				if (replacements.containsKey(variable))
					sb.append(replacements.get(variable));
				else
					sb.append(variableExp);
			}
			sb.append(value.substring(lastEnd));
			properties.setProperty((String) entry.getKey(), sb.toString());
		}
	}
	
	/**
	 * helper to handle parameter list of the form
	 * 
	 *  <code>key1=value1 @fileref1 key2=@fileref2 </code>  
	 * 
	 * where 
	 * <ul>
	 * <li>key=value is taken literally.
	 * <li>@fileref1 causes the named property file to be read
	 * <li>key=@fileref causes the value to be read from the named file  
	 * </ul>
	 * @param args
	 * @throws IOException
	 */
	public static Map<String,String>getParameters(String[] args) throws IOException {
		Map<String,String> params = new HashMap<String, String>();
		for(String arg: args){
			if(arg.startsWith("@")){
				Properties p = new Properties();
				
				try(FileInputStream fis = new FileInputStream(arg.substring(1))){
					p.load(fis);
					for(Object k : p.keySet()){
						String key = (String)k;
						params.put(String.valueOf(key), p.getProperty(key));
					}
				}
				continue;
			}
			else{
				String[]sp=arg.split("=",2);
				String name = sp[0];
				String val = sp[1];
				if(val.startsWith("@")){
					val=FileUtils.readFileToString(new File(val.substring(1)), "UTF-8");
				}
				params.put(name, val);
			}
		}
		return params;
	}

	
}
