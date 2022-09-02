package de.fzj.unicore.ucc.helpers;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.util.httpclient.IClientConfiguration;
import groovy.lang.Script;

/**
 * Base class for groovy scripts
 *
 * @author schuller
 */
public abstract class Base extends Script{

	IRegistryClient registry;
  	IClientConfiguration securityProperties;
  	String registryURL;
  	Properties properties;

	Options options;
	CommandLine commandLine;

}
