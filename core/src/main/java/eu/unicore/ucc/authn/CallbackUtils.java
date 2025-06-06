package eu.unicore.ucc.authn;

import java.io.IOException;

import eu.unicore.security.canl.CachingPasswordCallback;
import eu.unicore.security.canl.PasswordCallback;
import eu.unicore.security.wsutil.client.authn.UsernameCallback;
import eu.unicore.ucc.UCC;

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
		StringBuilder sb = new StringBuilder();
		sb.append("Please enter your ").append(protectedArtifactType);
		if(protectedArtifactDescription!=null){
			sb.append("(").append(protectedArtifactDescription).append(")");
		}
		sb.append(" password: ");
		System.out.println(sb.toString());
		return readLine(true).toCharArray();
	}

	public static String getUsernameFromUser() throws IOException {
		System.out.println("Please enter your username: ");
		return readLine(false);
	}

	public static String getUsernameFromUser(String protectedArtifactType) throws IOException {
		System.out.println("Please enter your "+ protectedArtifactType +" username: ");
		return readLine(false);
	}

	private static String readLine(boolean hidden) {
		if(UCC.unitTesting)return "test123";
		return UCC.getLineReader().readLine(hidden? '*' : null).trim();
	}

}
