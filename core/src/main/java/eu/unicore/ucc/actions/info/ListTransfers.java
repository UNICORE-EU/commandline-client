package eu.unicore.ucc.actions.info;

import java.util.Iterator;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.lookup.Blacklist;
import eu.unicore.client.lookup.CoreEndpointLister;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.shell.URLCompleter;
import eu.unicore.util.Log;

/**
 * lists server-to-server transfers
 *
 * @author schuller
 */
public class ListTransfers extends ListActionBase<BaseServiceClient> {

	public String getName(){
		return "list-transfers";
	}

	protected UnitParser unitParser=UnitParser.getCapacitiesParser(0);

	@Override
	public void process() throws Exception {
		super.process();
		CoreEndpointLister coreLister = new CoreEndpointLister(registry,configurationProvider,configurationProvider.getRESTAuthN());
		coreLister.setExecutor(UCC.executor);
		coreLister.setAddressFilter(new Blacklist(blacklist));
		for(CoreClient ep: coreLister){
			if(ep==null){
				if(!coreLister.isRunning()){
					break;
				}
			}
			else{
				String url = ep.getEndpoint().getUrl();
				URLCompleter.registerSiteURL(url);
				verbose("Site : "+ep.getEndpoint().getUrl());	
				try{
					listFiletransfers(ep);
				}catch(Exception ex){
					error("Error listing site at "+ep.getEndpoint().getUrl(), ex);
				}
			}
		};
	}

	protected void listFiletransfers(CoreClient ep)throws Exception{
		Endpoint ftEp = ep.getEndpoint().cloneTo(ep.getLinkUrl("transfers")); 
		EnumerationClient ftEnumeration = new EnumerationClient(ftEp, ep.getSecurityConfiguration(), ep.getAuth());
		
		Iterator<String> fts = ftEnumeration.iterator();
		while(fts.hasNext()){
			String eprd=fts.next();
			BaseServiceClient ftc = new BaseServiceClient(ftEp.cloneTo(eprd), 
					ftEnumeration.getSecurityConfiguration(), ftEnumeration.getAuth());
			if(filterMatch(ftc)){
				listFiletransfer(ftc);
				lastNumberOfResults++;
			}
		}
	}

	protected void listFiletransfer(BaseServiceClient ftc) throws Exception {
		try{
			message(ftc.getEndpoint().getUrl()+getDetails(ftc));
			properties.put(PROP_LAST_RESOURCE_URL, ftc.getEndpoint().getUrl());
		}catch(Exception ex){
			error("Error listing filetransfer at "+ftc.getEndpoint().getUrl(), ex);
		}
		printProperties(ftc);
	}

	@Override
	protected String getDetails(BaseServiceClient ftc) {
		if(!detailed)return "";
		StringBuilder sb=new StringBuilder();
		try{
			JSONObject props = ftc.getProperties();
			sb.append(" ");
			sb.append(props.getString("status"));
			sb.append(" ");
			sb.append(unitParser.getHumanReadable(Long.parseLong(props.getString("transferredBytes"))));
		}catch(Exception ex){sb.append(Log.createFaultMessage("Error", ex));}
		return sb.toString();
	}

	@Override
	public String getDescription(){
		return "list server-to-server transfers";
	}
	@Override
	public String getArgumentList(){
		return "";
	}
	@Override
	public String getCommandGroup(){
		return CMD_GRP_DATA;
	}
}
