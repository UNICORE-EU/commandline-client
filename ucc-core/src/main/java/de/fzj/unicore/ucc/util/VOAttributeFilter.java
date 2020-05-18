/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 27, 2011
 * Author: K. Stasiak <karol.m.stasiak@gmail.com>
 */
package de.fzj.unicore.ucc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import eu.unicore.samly2.attrprofile.ParsedAttribute;

/**
 * This class filters VO attributes based on inclusive and exclusive patterns.<br>
 * Patterns come in two flavors: name patterns and name-value patterns.<br>
 * Name patterns are simply regular expressions, that do not contain equals sign.
 * Name patterns are matched against attribute name. For each attribute,
 * if there is a matching exclusive name pattern, the attribute is dropped,
 * unless there is a matching inclusive name-value pattern.
 * If there is at least one inclusive name pattern, the attribute must match
 * at least one of them to pass.<br>
 * Name-value attributes consist of equals-sign-separated name part and value part.
 * Both of them are simply regular expressions.
 * (If there are multiple equals signs in the pattern, the first one is used.)
 * Name-value patterns match against both name and value of an attribute.
 * A value of an attribute passes if
 * <ul>
 * <li>matches all value-patterns of inclusive
 * name-value patterns that have name part that matches the name of the attribute
 * (in the case of zero such patterns, it passes)</li>
 * <li>matches no value-patterns of exclusive
 * name-value patterns that have name part that matches the name of the attribute</li>
 * </ul>
 * If none of values of an attribute passes, then the attribute itself is dropped.<br>
 * Filtered attribute list contains only attributes that passed and only those of
 * their values that passed.
 * @author Karol Stasiak
 */
public class VOAttributeFilter {

	/**
	 * An attribute filter instance that accepts all attributes and returns true 
	 * from {@link #allowsEverything() allowsEverything()} method.
	 */
	public static final VOAttributeFilter EMPTY_FILTER = new VOAttributeFilter(".*", ""){
		@Override
		public boolean allowsEverything(){
			return true;
		}
	};

	/**
	 * Factory method. If both parameters are null, returns 
	 * {@link #EMPTY_FILTER VOAttributeFilter.EMPTY_FILTER}.
	 * If not, returns new filter with those parameters.
	 * @param included semicolon-separated list of patterns, or null
	 * @param excluded semicolon-separated list of patterns, or null
	 */
	public static VOAttributeFilter build(String included, String excluded){
		if(included==null && excluded==null) return VOAttributeFilter.EMPTY_FILTER;
		return new VOAttributeFilter(included, excluded);
	}
	
	private List<AttributePattern> includedPatterns;
	private List<AttributePattern> excludedPatterns;
	
	/**
	 * @param included semicolon-separated list of patterns (defaults to ".*" if null)
	 * @param excluded semicolon-separated list of patterns (defaults to "" if null)
	 */
	public VOAttributeFilter(String included, String excluded){
		if(included==null) included=".*";
		if(excluded==null) excluded="";
		String[] i = included.split(";");
		String[] e = excluded.split(";");
		includedPatterns = new ArrayList<AttributePattern>(i.length);
		excludedPatterns = new ArrayList<AttributePattern>(e.length);
		for(String p: i){
			if(p.length()>0){
				includedPatterns.add(new AttributePattern(p));
			}
		}
		for(String p: e){
			if(p.length()>0){
				excludedPatterns.add(new AttributePattern(p));
			}
		}
	}

	/**
	 * Returns these attributes from given list that pass this filter's tests.
	 * @param attributes attribute list
	 * @return filtered attributes
	 */
	public List<ParsedAttribute> filter(List<ParsedAttribute> attributes){
		List<AttributeStatus> statuses = new ArrayList<AttributeStatus>(attributes.size());
		for(ParsedAttribute attr: attributes){
			statuses.add(new AttributeStatus(attr));
		}
		for(AttributePattern eap: excludedPatterns){
			for(AttributeStatus s: statuses){
				eap.excludeIfMatches(s);
			}
		}
		for(AttributePattern iap: includedPatterns){
			for(AttributeStatus s: statuses){
				iap.includeIfMatches(s);
			}
		}
		for(AttributeStatus s: statuses){
			s.process();
		}
		List<ParsedAttribute> result = new ArrayList<ParsedAttribute>();
		for(AttributeStatus s: statuses){
			if(s.attribute!=null) result.add(s.attribute);
		}
		return result;
	}

	@Override
	public String toString(){
		return "Included: "+includedPatterns.toString()+", Excluded: "+excludedPatterns.toString();
	}

	/**
	 * Returns true if caller can assume that calling {@link #filter(List) filter(x)} will always yield x.
	 * @return true if filter does not do anything
	 */
	public boolean allowsEverything(){
		return false;
	}
	
	private static class AttributePattern{
		Pattern namePattern;
		Pattern valuePattern;
		AttributePattern(String regex){
			if(regex.contains("=")){
				String[]parts = regex.split("=", 2);
				namePattern=Pattern.compile("^"+parts[0].trim()+"$");
				valuePattern=Pattern.compile("^"+parts[1].trim()+"$");
			}
			else{
				namePattern=Pattern.compile("^"+regex.trim()+"$");
				valuePattern=null;
			}
		}
		void excludeIfMatches(AttributeStatus status){
			if(!namePattern.matcher(status.attribute.getName()).matches()) return;
			if(valuePattern==null){
				status.excluded=true;
			}
			else{
				status.excludeValuePatterns.add(valuePattern);
			}
		}
		void includeIfMatches(AttributeStatus status){
			if(valuePattern==null) status.thereAreNoValuelessIncludePatterns=false;
			if(!namePattern.matcher(status.attribute.getName()).matches()) return;
			status.included=true;
			if(valuePattern!=null){
				status.includeValuePatterns.add(valuePattern);
			}
		}

		public String toString(){
			if(valuePattern!=null){
				return namePattern.pattern()+" = "+valuePattern.pattern();
			}
			else{
				return namePattern.pattern()+" = <anything>";
			}
		}
	}
	
	private static class AttributeStatus{
		ParsedAttribute attribute;
		boolean excluded=false;
		boolean included=false;
		boolean thereAreNoValuelessIncludePatterns=true;
		List<Pattern> excludeValuePatterns;
		List<Pattern> includeValuePatterns;

		AttributeStatus(ParsedAttribute at){
			attribute=at;
			excludeValuePatterns=new ArrayList<Pattern>();
			includeValuePatterns=new ArrayList<Pattern>();
		}
		
		void process(){
			if((excluded && includeValuePatterns.isEmpty())
					|| !(included||thereAreNoValuelessIncludePatterns)){
				attribute=null;
				return;
			}
			List<String> newValues=new ArrayList<String>();
			List<Object> newObjValues=new ArrayList<Object>();
			for (int i=0; i<attribute.getStringValues().size(); i++){
				String v = attribute.getStringValues().get(i);
				boolean ok=true;
				for(Pattern evp: excludeValuePatterns){
					if (evp.matcher(v).matches()){
						ok=false;
						break;
					}
				}
				if(ok && !includeValuePatterns.isEmpty()){
					ok=false;
					for(Pattern ivp: includeValuePatterns){
						if (ivp.matcher(v).matches()){
							ok=true;
							break;
						}
					}
				}
				if(ok) {
					newValues.add(v);
					if (!attribute.getDataType().equals(String.class))
						newObjValues.add(attribute.getObjectValues().get(i));
				}
			}//end for
			if(newValues.isEmpty()) {
				attribute=null;
			}
			else {
				if(newValues.size() != attribute.getStringValues().size()) {
					if (!attribute.getDataType().equals(String.class))
						attribute = new ParsedAttribute(attribute.getName(), attribute.getDescription(),
							newValues, newObjValues, attribute.getDataType());
					else
						attribute = new ParsedAttribute(attribute.getName(), attribute.getDescription(),
								newValues);
				}
			}
		}
	}
}
