package eu.unicore.ucc.lookup;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.lookup.Blacklist;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.shell.URLCompleter;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.util.Log;

/**
 * connects to the federation and creates target systems if required
 * 
 * @author schuller
 */
public class Connector implements Runnable {

	private final IRegistryClient registry;

	private final UCCConfigurationProvider cfgProvider;

	final AtomicInteger tsfAvailable=new AtomicInteger(0);
	final AtomicInteger tssAvailable=new AtomicInteger(0);

	/**
	 * blacklisted sites to avoid
	 */
	private String[] blacklist;

	/**
	 * @param registry
	 * @param cfgProvider
	 * @param msg
	 */
	public Connector(IRegistryClient registry, UCCConfigurationProvider cfgProvider){
		this.registry=registry;
		this.cfgProvider=cfgProvider;
	}

	@Override
	public void run() {
		SiteFactoryLister lister=new SiteFactoryLister(UCC.executor,registry,cfgProvider);
		if(blacklist!=null && blacklist.length>0){
			UCC.getConsoleLogger().verbose("Using blacklist <"+Arrays.asList(blacklist)+">");
			lister.setAddressFilter(new Blacklist(blacklist));
		}
		for(SiteFactoryClient tsf: lister){
			if(tsf == null){
				if(!lister.isRunning())
					break;
			}
			else{
				UCC.getConsoleLogger().verbose("Connecting to "+tsf.getEndpoint().getUrl());
				try{
					handleTSF(tsf);
					tsfAvailable.incrementAndGet();
				}catch(Exception ex){
					UCC.getConsoleLogger().error("Error creating site at "+tsf.getEndpoint().getUrl(),ex);
				}
			}
		}
	}
	
	protected void handleTSF(SiteFactoryClient tsf) throws Exception {
		try {
			SiteClient tss = tsf.getOrCreateSite();
			UCC.getConsoleLogger().verbose("TSS at address "+tss.getEndpoint().getUrl());
			_last_TSS = tss.getEndpoint().getUrl();
			URLCompleter.registerSiteURL(_last_TSS);
			tssAvailable.incrementAndGet();
		}catch(Exception e){
			if(Log.getDetailMessage(e).contains("Access denied")){
				UCC.getConsoleLogger().verbose("Access denied on <"+tsf.getEndpoint().getUrl()+">");
			}
			else{
				UCC.getConsoleLogger().error("Can't create target system.",e);
			}
		}
	}

	/**
	 * set the blacklist i.e. patterns of site URLs to ignore
	 * @param blackList
	 */
	public void setBlacklist(String[] blacklist){
		this.blacklist = blacklist;
	}

	public int getAvailableTSS(){
		return tssAvailable.get();
	}

	public int getAvailableTSF(){
		return tsfAvailable.get();
	}

	public static String _last_TSS = null;

}
