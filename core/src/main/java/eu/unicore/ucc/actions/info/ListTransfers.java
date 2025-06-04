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

	@Override
	public String getName(){
		return "list-transfers";
	}

	private final UnitParser unitParser=UnitParser.getCapacitiesParser(0);

	@Override
	public void process() throws Exception {
		super.process();
		CoreEndpointLister coreLister = new CoreEndpointLister(registry,configurationProvider,
				configurationProvider.getRESTAuthN(), UCC.executor);
		coreLister.setAddressFilter(new Blacklist(blacklist));
		if(detailed)printHeader();
		for(CoreClient ep: coreLister){
			if(ep==null){
				if(!coreLister.isRunning()){
					break;
				}
			}
			else{
				String url = ep.getEndpoint().getUrl();
				URLCompleter.registerSiteURL(url);
				try{
					listFiletransfers(ep);
				}catch(Exception ex){
					console.error(ex, "Error listing site at {}", ep.getEndpoint().getUrl());
				}
			}
		};
	}

	private void listFiletransfers(CoreClient ep)throws Exception{
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

	private void listFiletransfer(BaseServiceClient ftc) throws Exception {
		try{
			console.info("{}", getDetails(ftc));
			properties.put(PROP_LAST_RESOURCE_URL, ftc.getEndpoint().getUrl());
		}catch(Exception ex){
			console.error(ex,"Error listing filetransfer at {}", ftc.getEndpoint().getUrl());
		}
		printProperties(ftc);
	}

	String format = " %10s | %11s | %s";

	private void printHeader() {
		console.info(String.format(format, "Status", "Transmitted", "URL"));
		console.info("  ----------|-------------|----------------");
	}

	@Override
	protected String getDetails(BaseServiceClient ftc) {
		if(!detailed)return "";
		String url = ftc.getEndpoint().getUrl();
		try{
			JSONObject props = ftc.getProperties();
			String size = unitParser.getHumanReadable(Long.parseLong(String.valueOf(props.get("transferredBytes"))));
			return String.format(format, props.getString("status"), size, url);
		}catch(Exception ex){return Log.createFaultMessage("Error <"+url+"> ", ex);}
	}

	@Override
	public String getDescription(){
		return "list server-to-server transfers";
	}

	@Override
	public String getSynopsis(){
		return "Prints a list  of all your server-to-server transfers. "
				+"Use the -l option to show details.";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_DATA;
	}
}
