package de.fzj.unicore.ucc.util;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.lookup.AddressFilter;

public class Blacklist implements AddressFilter<TSFClient>{

	private final String[] patterns;

	public Blacklist(String[]patterns){
		this.patterns=patterns;
	}
	@Override
	public boolean accept(EndpointReferenceType epr) {
		return accept(epr.getAddress().getStringValue());
	}

	@Override
	public boolean accept(String uri) {
		if(patterns==null||patterns.length==0)return true;
		for(String p: patterns){
			if(uri.contains(p))return false;
		}
		return true;
	}

	@Override
	public boolean accept(TSFClient client) throws Exception {
		return true;
	}

}