package eu.unicore.ucc.util;

import eu.unicore.ucc.Command;


public class MockLoadClass extends Command {

	public static boolean loaded=false;
	
	static{
		loaded=true;
	}
	
	@Override
	public String getName() {
		return "MOCK2";
	}

	@Override
	public String getSynopsis() {
		return "this does not do anything either";
	}

	@Override
	public String getDescription() {
		return "a second mock command";
	}

}
