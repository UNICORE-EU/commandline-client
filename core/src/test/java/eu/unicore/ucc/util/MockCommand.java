package eu.unicore.ucc.util;

import eu.unicore.ucc.Command;


public class MockCommand extends Command {

	@Override
	public String getName() {
		return "MOCK";
	}

	@Override
	public String getSynopsis() {
		return "this does absolutely nothing";
	}

	@Override
	public String getDescription() {
		return "mock command";
	}

}
