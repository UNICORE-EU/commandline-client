package eu.unicore.ucc.runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.lookup.Blacklist;
import eu.unicore.client.lookup.SiteNameFilter;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.uas.json.Requirement;
import eu.unicore.ucc.Constants;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.helpers.ConsoleLogger;
import eu.unicore.ucc.lookup.SiteLister;
import eu.unicore.ucc.util.UCCBuilder;
import eu.unicore.util.Log;

/**
 * Local broker implementation: selects a suitable target system by looking
 * into the registry and matching requirements
 * 
 * @author schuller
 */
public class TargetSystemFinder implements Broker, Constants {

	@Override
	public int getPriority(){
		return 1;
	}

	@Override
	public String getName(){
		return "LOCAL";
	}

	@Override
	public Endpoint findTSSAddress(final IRegistryClient registry, 
			final UCCConfigurationProvider configurationProvider, UCCBuilder builder, SiteSelectionStrategy selectionStrategy)
					throws Exception {
		ConsoleLogger msg=builder.getMessageWriter();
		if(selectionStrategy==null)selectionStrategy = new RandomSelection();
		final List<SiteClient>available = listSites(registry, configurationProvider, builder);
		SiteClient tss=null;
		if(available.size()==0){
			throw new Exception("No matching target system available (try 'connect' or check job requirements)");
		}
		else{
			if(msg.isVerbose()){
				msg.verbose("Have <{}> candidate resource(s)", available.size());
				for(SiteClient tsc: available){
					msg.verbose("  {}", tsc.getEndpoint().getUrl());
				}
			}
			//select one
			tss=selectionStrategy.select(available);
		}
		msg.verbose("Selected TSS at {}", tss.getEndpoint().getUrl());
		return tss.getEndpoint();
	}

	@Override
	public Collection<Endpoint>listCandidates(IRegistryClient registry, 
			final UCCConfigurationProvider configurationProvider, UCCBuilder builder) 
					throws Exception{
		List<Endpoint>result=new ArrayList<>();
		List<SiteClient> sites = listSites(registry, configurationProvider, builder);
		for(SiteClient site: sites){
			result.add(site.getEndpoint());
		}
		return result;
	}

	private List<SiteClient> listSites(final IRegistryClient registry, 
			final UCCConfigurationProvider configurationProvider, UCCBuilder builder)
					throws Exception{
		final Collection<Requirement> requirements = builder.getRequirements();
		final String siteName=builder.getSite();
		final ConsoleLogger msg=builder.getMessageWriter();
		final List<SiteClient>available=Collections.synchronizedList(new ArrayList<>());

		final String blackList = builder.getProperty("blacklist"); 
		final boolean checkResources=true;

		SiteLister tsfList = new SiteLister(UCC.executor,registry,configurationProvider);
		if(blackList!=null){
			String[] blacklist=blackList.trim().split(" ");
			tsfList.setAddressFilter(new Blacklist(blacklist));
		}

		if(siteName!=null){
			tsfList.setAddressFilter(new SiteNameFilter(siteName));
		}

		for(SiteClient tsf: tsfList){
			if(tsf == null){
				if(!tsfList.isRunning())
					break;
			}
			else{
				String current = tsf.getEndpoint().getUrl();
				msg.verbose("Checking "+current);
				try{
					ErrorHolder err = new ErrorHolder();
					if(!matches(tsf, requirements, err, checkResources, msg)) {
						msg.verbose("Skipped {}: {}", current, err.message);
					}
					else {
						available.add(tsf);
					}
				} catch(Exception ex) {
					msg.error(ex, "Error on TSF {}", current);
				}
			}
		}
		return available;
	}

	/**
	 * check resource requirements
	 */
	private boolean matches(SiteClient tssClient, Collection<Requirement> requirements, ErrorHolder error, boolean checkResources, ConsoleLogger msg){
		try{
			if(!checkResources || requirements==null || requirements.size()==0){
				return true;
			}
			JSONObject props = tssClient.getProperties();
			for(Requirement r: requirements){
				msg.verbose("Check requirement: "+r.getDescription());
				if(r.isFulfilled(props))continue;
				error.message = "Requirement <"+r.getDescription()+"> not fulfilled on "+tssClient.getEndpoint().getUrl();
				return false;
			}
			return true;
		}catch(Exception e){
			error.message = Log.createFaultMessage("Can't contact target system", e);
			return false;
		}
	}

	static class ErrorHolder {
		String message;
	}

}
