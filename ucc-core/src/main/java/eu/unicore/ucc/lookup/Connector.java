package eu.unicore.ucc.lookup;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.lookup.Blacklist;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.util.Log;

/**
 * connects to the federation and creates target systems if required
 * 
 * @author schuller
 */
public class Connector implements Runnable {

	private final IRegistryClient registry;

	private final UCCConfigurationProvider cfgProvider;

	private final MessageWriter msg;

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
	public Connector(IRegistryClient registry, UCCConfigurationProvider cfgProvider, MessageWriter msg){
		this.registry=registry;
		this.cfgProvider=cfgProvider;
		this.msg=msg;
	}

	@Override
	public void run() {
		SiteFactoryLister lister=new SiteFactoryLister(UCC.executor,registry,cfgProvider);
		if(blacklist!=null && blacklist.length>0){
			msg.verbose("Using blacklist <"+Arrays.asList(blacklist)+">");
			lister.setAddressFilter(new Blacklist(blacklist));
		}

		for(SiteFactoryClient tsf: lister){
			if(tsf == null){
				if(!lister.isRunning())
					break;
			}
			else{
				msg.verbose("Connecting to "+tsf.getEndpoint().getUrl());
				try{
					handleTSF(tsf);
					tsfAvailable.incrementAndGet();
				}catch(Exception ex){
					msg.error("Error on TSF "+tsf.getEndpoint().getUrl(),ex);
				}
			}
		}
	}
	
	protected void handleTSF(SiteFactoryClient tsf) throws Exception {
		try {
			SiteClient tss = tsf.getOrCreateSite();
			msg.verbose("Created TSS at address "+tss.getEndpoint().getUrl());
			_last_TSS = tss.getEndpoint().getUrl();
			tssAvailable.incrementAndGet();
		}catch(Exception e){
			if(Log.getDetailMessage(e).contains("Access denied")){
				msg.verbose("Access denied on <"+tsf.getEndpoint().getUrl()+">");
			}
			else{
				msg.error("Can't create target system.",e);
			}
		}
	}

	/**
	 * set the blacklist i.e. pattern of 
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
