package de.fzj.unicore.ucc.util;

import de.fzj.unicore.ucc.Command;


public class MockLoadClass extends Command {

	public static boolean loaded=false;
	
	static{
		loaded=true;
	}
	
}
