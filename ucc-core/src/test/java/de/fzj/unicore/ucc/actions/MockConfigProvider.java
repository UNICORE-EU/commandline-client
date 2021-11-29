/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.ucc.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.security.wsutil.client.authn.AuthenticationProvider;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.security.wsutil.client.authn.SecuritySessionPersistence;
import eu.unicore.security.wsutil.client.authn.ServiceIdentityResolver;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;


public class MockConfigProvider implements UCCConfigurationProvider
{
	private DefaultClientConfiguration configuration;
	private DefaultClientConfiguration anonConfiguration;
	
	public MockConfigProvider(DefaultClientConfiguration configuration)
	{
		this.configuration = configuration;
		this.anonConfiguration = configuration.clone();
		anonConfiguration.setCredential(null);
	}

	@Override
	public IClientConfiguration getClientConfiguration(String serviceUrl)
			throws Exception
	{
		return configuration;
	}
	
	@Override
	public IClientConfiguration getClientConfiguration(String serviceUrl, String serviceIdentity, 
			DelegationSpecification delegate)
			throws Exception
	{
		return configuration;
	}

	@Override
	public void flushSessions() throws IOException
	{
	}

	@Override
	public IClientConfiguration getAnonymousClientConfiguration() throws Exception
	{
		return anonConfiguration;
	}

	@Override
	public IClientConfiguration getBasicClientConfiguration()
	{
		return anonConfiguration;
	}

	@Override
	public Map<String, String[]> getSecurityPreferences()
	{
		return new HashMap<String, String[]>();
	}

	@Override
	public AuthenticationProvider getAuthnProvider()
	{
		return null;
	}
	
	@Override
	public IAuthCallback getRESTAuthN()
	{
		return null;
	}

	@Override
	public SecuritySessionPersistence getSessionsPersistence()
	{
		return null;
	}

	@Override
	public Properties getUserProperties()
	{
		return null;
	}


	@Override
	public ServiceIdentityResolver getIdentityResolver()
	{
		return null;
	}

}
