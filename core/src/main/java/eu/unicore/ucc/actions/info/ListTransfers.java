package eu.unicore.ucc.actions.info;

import org.json.JSONObject;

import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.lookup.SiteFilter;
import eu.unicore.ucc.lookup.TransferLister;
import eu.unicore.util.Log;

/**
 * lists server-to-server transfers
 *
 * @author schuller
 */
public class ListTransfers extends ListActionBase<BaseServiceClient> {

	private final UnitParser unitParser = UnitParser.getCapacitiesParser(0);

	@Override
	public String getName(){
		return "list-transfers";
	}

	@Override
	public void setupOptions() {
		super.setupOptions();
		siteName = getCommandLine().getOptionValue(OPT_SITENAME);
		if(detailed)printHeader();
	}

	@Override
	protected Iterable<BaseServiceClient>iterator()throws Exception {
		TransferLister lister = new TransferLister(UCC.executor, registry, configurationProvider, tags);
		lister.setAddressFilter(new SiteFilter(siteName, blacklist));
		return () -> lister.iterator(); 
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
