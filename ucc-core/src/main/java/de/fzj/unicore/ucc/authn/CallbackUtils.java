/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.ucc.authn;

import java.io.IOException;

import eu.unicore.security.canl.CachingPasswordCallback;
import eu.unicore.security.canl.PasswordCallback;
import eu.unicore.security.wsutil.client.authn.UsernameCallback;
import jline.console.ConsoleReader;

public class CallbackUtils
{
	public static PasswordCallback getPasswordCallback()
	{
		return new CachingPasswordCallback()
		{
			@Override
			public boolean ignoreProperties()
			{
				return false;
			}

			@Override
			public char[] getPasswordFromUser(String protectedArtifactType,
					String protectedArtifactDescription)
			{
				return getPasswordFromUserCmd(protectedArtifactType,
								protectedArtifactDescription);
			}

			@Override
			public boolean askForSeparateKeyPassword()
			{
				// TODO - or true?
				return false;
			}
		};
	}
	
	public static UsernameCallback getUsernameCallback()
	{
		return new UsernameCallback()
		{
			private String username = null;
			
			@Override
			public String getUsername()
			{
				if (username != null)
					return username;
				try
				{
					username = getUsernameFromUser();
				} catch (IOException e)
				{
					return null;
				}
				return username;
			}
		};
	}

	public static char[] getPasswordFromUserCmd(String protectedArtifactType, String protectedArtifactDescription) {
		String r;
		StringBuilder sb = new StringBuilder();
		sb.append("Please enter your ").append(protectedArtifactType);
		if(protectedArtifactDescription!=null){
			sb.append("(").append(protectedArtifactDescription).append(")");
		}
		sb.append(" password: ");
		System.out.println(sb.toString());
		
		ConsoleReader cr = null;
		try {
			cr = new ConsoleReader();
			cr.setExpandEvents(false);
			r = cr.readLine(Character.valueOf('*'));
		} catch (IOException e) {
			return null;
		} finally{
			if(cr!=null)cr.shutdown();
		}
		r.trim();
		return r.toCharArray();
	}
	
	public static String getUsernameFromUser() throws IOException {
		String r;
		ConsoleReader cr = null; 
		try {
			cr = new ConsoleReader();
			System.out.println("Please enter your username: ");
			r = cr.readLine();
			r.trim();
			return r;
		}finally {
			if(cr!=null) cr.shutdown();
		}
	}
	
	public static String getUsernameFromUser(String protectedArtifactType) throws IOException {
		String r;
		ConsoleReader cr = null; 
		try {
			cr = new ConsoleReader();
			System.out.println("Please enter your "+ protectedArtifactType +" username: ");
			r = cr.readLine();
			r.trim();
			return r;
		}finally {
			if(cr!=null) cr.shutdown();
		}
	}

}
