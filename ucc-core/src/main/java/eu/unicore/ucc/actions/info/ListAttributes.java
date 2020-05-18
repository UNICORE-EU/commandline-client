/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Sep 5, 2011
 * Author: K. Stasiak <karol.m.stasiak@gmail.com>
 */

package eu.unicore.ucc.actions.info;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.fzj.unicore.ucc.authn.oidc.OIDCAgentAuthN;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.assertion.AssertionParser;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile.ScopedStringValue;
import eu.unicore.security.wsutil.client.SAMLAttributePushOutHandler;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.security.wsutil.client.authn.SAMLAuthN;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Action for displaying attributes retrieved from Unity (when using Unity based Authn)
 * @author Karol Stasiak
 * @author K. Benedyczak
 */
public class ListAttributes extends ActionBase {
	@Override
	public String getName() {
		return "list-attributes";
	}

	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
	}
	
	@Override
	public String getDescription() {
		return "list user attributes retrieved from Unity";
	}

	@Override
	public String getCommandGroup(){
		return "Security";
	}

	@Override
	public String getSynopsis() {
		return "If using Unity for authentication, the attributes coming from Unity are listed. "
			+ "All attributes are displayed in human readable table. The table also indicates" 
			+ " whether the attribute would be included or not when using the scope and filter settings, "
			+"which were provided to this command.";
	}

	@Override
	protected boolean skipConnectingToRegistry(){
		return true;
	}

	/**
	 * The minimum width of output table.
	 */
	private static final int MINIMUM_TABLE_WIDTH = 63;

	public void prettyPrint(List<ParsedAttribute> la){
		ArrayList<String> scopeColumn = new ArrayList<String>();
		ArrayList<String> nameColumn = new ArrayList<String>();
		ArrayList<String> valueColumn = new ArrayList<String>();
		ArrayList<String> filteredColumn = new ArrayList<String>();
		ArrayList<String> descriptions = new ArrayList<String>();
		scopeColumn.add("Scope");
		nameColumn.add("Name");
		valueColumn.add("Value");
		filteredColumn.add("F");
		Collections.sort(la, ATTRIBUTE_COMPARATOR);
		
		String latestName="";
		for(ParsedAttribute attr: la){
			List<String> attrValue = attr.getStringValues();
			String attrScope = getScope(attr);
				
			String attrName = attr.getName();
			if(equal(latestName,attrName)){
				attrName="";
			}
			else {
				//horizontal line
				scopeColumn.add(null);
				nameColumn.add(null);
				valueColumn.add(null);
				filteredColumn.add(null);

				latestName=attrName;

				String description = attr.getDescription();
				if(description!=null && !"".equals(description)){
					// rows with name starting with "<D>" are considered placeholders for attribute descriptions
					// description ID are stored as strings in value column and refer to 'descriptions' ArrayList
					nameColumn.add("<D>");
					valueColumn.add(Integer.toString(descriptions.size()));
					scopeColumn.add("");
					filteredColumn.add("");
					descriptions.add(description);
	
					scopeColumn.add(null);
					nameColumn.add(null);
					valueColumn.add(null);
					filteredColumn.add(null);
				}
			}
			nameColumn.add(attrName);
			if(attrScope==null) attrScope="<GLOBAL>";
			scopeColumn.add(attrScope);
			boolean scopeFiltered = false;
			
			for(int cc=1; cc<attrValue.size(); cc++){
				nameColumn.add("");
				scopeColumn.add("");
			}
			
			List<String> filteredValues = Collections.emptyList();
			boolean attributeFullyFiltered = true;
			for(ParsedAttribute filteredAttr: la) {
				String fAttrScope = getScope(filteredAttr);
				if (equal(filteredAttr.getName(), attr.getName()) &&
					equal(fAttrScope, attrScope)){
					filteredValues=filteredAttr.getStringValues();
					attributeFullyFiltered = false; 
					break;
				}
			}
			if(attrValue.isEmpty()){
				valueColumn.add("<no values>");
				if (!scopeFiltered && !attributeFullyFiltered)
					filteredColumn.add(" ");
				else if (scopeFiltered && !attributeFullyFiltered)
					filteredColumn.add("S");
				else if (!scopeFiltered && attributeFullyFiltered)
					filteredColumn.add("N");
				else
					filteredColumn.add("B");
			}
			for(String v: attrValue){
				valueColumn.add(v);
				if(filteredValues.contains(v)){
					if (!scopeFiltered)
						filteredColumn.add(" ");
					else
						filteredColumn.add("S");
				} else {
					if (!scopeFiltered)
						filteredColumn.add("N");
					else
						filteredColumn.add("B");
				}
			}
		}
		//horizontal line
		scopeColumn.add(null);
		nameColumn.add(null);
		valueColumn.add(null);
		filteredColumn.add(null);
		
		
		
		normalize(scopeColumn,0);
		normalize(nameColumn,0);
		normalize(valueColumn,0);
		normalize(filteredColumn,
				MINIMUM_TABLE_WIDTH
				-nameColumn.get(0).length()
				-scopeColumn.get(0).length()
				-valueColumn.get(0).length()
				-3 //for 3 column separators
				);
		for (int i=0; i<nameColumn.size(); i++){
			if(nameColumn.get(i).startsWith("<D>")){
				int width = scopeColumn.get(i).length() + nameColumn.get(i).length() + filteredColumn.get(i).length() + valueColumn.get(i).length() + 3;
				message(wrap(descriptions.get(Integer.parseInt(valueColumn.get(i).trim())), width));
			}
			else {
				message(nameColumn.get(i) + "|" + scopeColumn.get(i) + "|" + valueColumn.get(i) + "|" + filteredColumn.get(i));
			}
		}
		message("");
		message("The last column shows whether the attribute would be excluded by the current " +
				"filtering settings, which were:");
		
		message("Legend: S - excluded by scope, N - excluded by name/value, B - excluded by both scope and name/value");
	}
	
	@Override
	public void process() {
		super.process();
		if(SAMLAuthN.NAME.equalsIgnoreCase(authNMethod)
			|| OIDCAgentAuthN.NAME.equalsIgnoreCase(authNMethod))
		{
			try{
				processUnityAttribs();
			}catch (Exception e) {
				error("Problem printing attributes", e);
				endProcessing(ERROR);
			}
		}
		else{
			error("Not using Unity for authentication.", null);
			endProcessing(ERROR);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void processUnityAttribs() throws Exception {
		message("Listing attributes from Unity");
		List<ParsedAttribute>attribs = new ArrayList<ParsedAttribute>();
		IClientConfiguration secProps = configurationProvider.getClientConfiguration("https://dummy.net", "", 
				DelegationSpecification.DO_NOT);
		
		List<Assertion> assertionsToBePushed = (List<Assertion>)secProps.getExtraSecurityTokens().get(SAMLAttributePushOutHandler.PUSHED_ASSERTIONS);
		message("Have "+assertionsToBePushed.size()+" assertions.");
		if(assertionsToBePushed.size()>0){
			printInfo(assertionsToBePushed.get(0));
		}
		for(Assertion a: assertionsToBePushed){
			attribs.addAll(new AttributeAssertionParser(a.getXMLBean()).getAttributes());
		}
		
		prettyPrint(UVOSAttributeProfile.splitByScopes(attribs));
	}

	private void printInfo(AssertionParser a){
		String subj = a.getSubjectName();
		String iss = a.getIssuerName();
		verbose(String.format("Attributes for %s issued by %s", subj, iss));
	}

	
	private String getScope(ParsedAttribute attr)
	{
		if (attr.getDataType().isAssignableFrom(ScopedStringValue.class) && 
				!attr.getObjectValues().isEmpty())
			return ((ScopedStringValue)attr.getObjectValues().get(0)).getScope();
		return null;
	}
	
	/**
	 * Pads each non-null element in the arraylist with spaces
	 * to the length of the longest element in the arraylist,
	 * but not less than the minimal width.
	 * Replaces null elements in the arraylist with series of dashes
	 * of the same length.
	 * The arraylist is modified in place.
	 * @param list the list of strings
	 * @param minWidth minimum length of strings after the normalization
	 */
	private static void normalize(ArrayList<String> list, int minWidth){
		int max=minWidth;
		for(String s:list){
			if(s!=null && s.length()>max) max=s.length();
		}
		char[] tmp=new char[max];
		Arrays.fill(tmp, ' ');
		String spaces=new String(tmp);
		Arrays.fill(tmp, '-');
		String minuses=new String(tmp);
		for(int i=0; i<list.size(); i++){
			if(list.get(i)==null){
				list.set(i,minuses);
			}
			else {
				int l=list.get(i).length();
				if(l<max){
					list.set(i,list.get(i)+spaces.substring(0, max-l));
				}
			}
		}
	}

	private static boolean equal(String s1, String s2){
		return (s1==null && s2==null)||(s1!=null && s1.equals(s2));
	}

	public static final Comparator<ParsedAttribute> ATTRIBUTE_COMPARATOR
			= new Comparator<ParsedAttribute>(){
		@Override
		public int compare(ParsedAttribute t, ParsedAttribute t1) {
			int n = t.getName().compareTo(t1.getName());
			if (n!=0) return n;
			String scope1 = null;
			String scope2 = null;
			if (t.getDataType().isAssignableFrom(ScopedStringValue.class) && !t.getObjectValues().isEmpty())
				scope1 = ((ScopedStringValue)t.getObjectValues().get(0)).getScope();
			if (t1.getDataType().isAssignableFrom(ScopedStringValue.class) && !t1.getObjectValues().isEmpty())
				scope2 = ((ScopedStringValue)t1.getObjectValues().get(0)).getScope();
			
			if(scope1==null && scope2!=null){
				return -1;
			}
			if(scope1!=null && scope2==null){
				return +1;
			}
			if(scope1==null && scope2==null){
				return 0;
			}
			return scope1.compareTo(scope2);
		}
	};
	
	
	private String wrap(String orig, int width){
		return orig;
	}
}
