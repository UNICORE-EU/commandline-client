package eu.unicore.ucc.util;

import eu.unicore.ucc.Command;


public class MockLoadClass extends Command {

	public static boolean loaded=false;
	
	static{
		loaded=true;
	}
	
}
