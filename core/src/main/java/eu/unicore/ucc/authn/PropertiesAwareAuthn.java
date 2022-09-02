/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.ucc.authn;

import java.util.Properties;

import eu.emi.security.authn.x509.ValidationErrorListener;

/**
 * Impls will have their properties set
 * @author K. Benedyczak
 */
public interface PropertiesAwareAuthn
{
	public void setProperties(Properties properties);
	
    public void setValidationErrorListener(ValidationErrorListener properties);

}
