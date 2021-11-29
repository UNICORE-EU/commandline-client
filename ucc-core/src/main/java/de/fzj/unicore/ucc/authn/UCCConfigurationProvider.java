/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.ucc.authn;

import java.util.Properties;

import eu.unicore.security.wsutil.client.authn.ClientConfigurationProvider;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * UCC specific extension of {@link WSRFClientConfigurationProvider}
 * allowing for property-file based configuration
 * 
 * @author K. Benedyczak
 */
public interface UCCConfigurationProvider extends ClientConfigurationProvider
{
	public Properties getUserProperties();
	
	public IAuthCallback getRESTAuthN();

	public IClientConfiguration getClientConfiguration(String url) throws Exception;
}
