/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Oct 6, 2011
 * Author: K. Stasiak <karol.m.stasiak@gmail.com>
 */
package de.fzj.unicore.ucc.helpers;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.ucc.util.VOAttributeFilter;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AttributeAssertionGenerator;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile.ScopedStringValue;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.elements.SAMLAttribute;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.wsutil.samlclient.SAMLAttributeQueryClient;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Helper class for fetching attribute assertions from VO server.
 * @author kstasiak
 */
public class AttributeAssertionFetcher {
	
	private static final Logger logger = Log.getLogger("UCC", AttributeAssertionFetcher.class);

	private final IClientConfiguration cfg;

	private final SAMLAttributeQueryClient client;

	/**
	 * @param cfg client configuration
	 * @param voURL URL of VO server
	 * @throws MalformedURLException
	 */
	public AttributeAssertionFetcher(IClientConfiguration cfg, String voURL) throws MalformedURLException {
		client = new SAMLAttributeQueryClient(voURL, cfg);
		this.cfg = cfg;
	}

	/**
	 * Fetches an attribute assertion from VO server.
	 * If the filter does not report that it allows all attributes
	 * (by returning true from {@link VOAttributeFilter#allowsEverything()}),
	 * the attribute list is fetched from server, filtered and server is queried again for the list.
	 * @param scope the scope of attributes, VO and group
	 * @param attributeFilter attribute filter
	 * @return assertion containing attributes in given scope and accepted by given filter
	 * @throws SAMLValidationException
	 */
	public AttributeAssertionParser fetchAssertion(String scope, VOAttributeFilter attributeFilter) 
			throws SAMLValidationException {
		String userDn = getUserDN();
		if(logger.isDebugEnabled()) {
			logger.debug("User identity: " + userDn!=null? X500NameUtils.getReadableForm(userDn): "n/a");
			if (scope != null) {
				logger.debug("Assuming group " + scope);
			} else {
				logger.debug("Scope (group) was not selected, fetching attributes from all groups");
			}
		}
		NameID identity = new NameID(userDn, SAMLConstants.NFORMAT_DN);
		AttributeAssertionParser assertionParser = client.getAssertion(identity, identity);
		if (scope == null && (attributeFilter == null || attributeFilter.allowsEverything()))
		{
			if(logger.isTraceEnabled()) {
				logger.trace("Attribute assertion fetched:");
				logger.trace(assertionParser.getXMLBeanDoc().toString());
			}
			return assertionParser;
		}

		List<ParsedAttribute> allAttributes = assertionParser.getAttributes();
		allAttributes = UVOSAttributeProfile.splitByScopes(allAttributes);
		filterByScope(scope, allAttributes);
		List<ParsedAttribute> filtered = attributeFilter.filter(allAttributes);
		Set<SAMLAttribute> interestingAttributes = new HashSet<SAMLAttribute>();
		AttributeAssertionGenerator generator = new AttributeAssertionGenerator();
		generator.addProfile(new UVOSAttributeProfile());
		for (ParsedAttribute pa: filtered)
			interestingAttributes.add(new SAMLAttribute(generator.getAttribute(pa)));
		assertionParser = client.getAssertion(identity, identity, interestingAttributes);
		if(logger.isTraceEnabled()){
			logger.trace("Filtered attribute assertion fetched:");
			logger.trace(assertionParser.getXMLBeanDoc().toString());
		}
		return assertionParser;
	}

	private String getUserDN(){
		if(cfg.getCredential()!=null){
			return cfg.getCredential().getSubjectName();
		}
		else if(cfg.getETDSettings()!=null && cfg.getETDSettings().getRequestedUser()!=null){
			return cfg.getETDSettings().getRequestedUser();
		}
		return null;
	}

	private void filterByScope(String scope, List<ParsedAttribute> allAttributes)
	{
		if (scope == null)
			return;
		for (int i=allAttributes.size()-1; i>=0; i--)
		{
			ParsedAttribute a = allAttributes.get(i);
			if (a.getDataType().isAssignableFrom(ScopedStringValue.class) && !a.getObjectValues().isEmpty())
			{
				String attrScope = ((ScopedStringValue)a.getObjectValues().get(0)).getScope(); 
				if (attrScope != null && !attrScope.startsWith(scope))
					allAttributes.remove(i);
			}
		}
	}
}
