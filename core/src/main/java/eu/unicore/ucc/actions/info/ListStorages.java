package eu.unicore.ucc.actions.info;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.lookup.SiteFilter;
import eu.unicore.ucc.lookup.StorageLister;

public class ListStorages extends ListActionBase<StorageClient> {

	private final UnitParser unitParser=UnitParser.getCapacitiesParser(1);

	private boolean showAll = false;

	@Override
	public String getName(){
		return "list-storages";
	}

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_ALL)
				.longOpt(OPT_ALL_LONG)
				.desc("Show all storages including job directories")
				.required(false)
				.get());
	}

	@Override
	protected void setupOptions() {
		super.setupOptions();
		this.showAll = getCommandLine().hasOption(OPT_ALL);
		console.debug("Listing job directories = {}", showAll);
	}

	@Override
	protected Iterable<StorageClient>iterator()throws Exception {
		// do we have a list?
		if(getCommandLine().getArgList().size()>1){
			List<StorageClient>storages = new ArrayList<>();
			getCommandLine().getArgList().listIterator(1).forEachRemaining(
				(url)-> {
						try{
							storages.add(makeClient(String.valueOf(url)));
						}catch(Exception e) {
							UCC.console.error(e, "Cannot create client for {}", url);
						}
				});
			return storages;
		}
		else {
			StorageLister storageLister = new StorageLister(UCC.executor, registry, configurationProvider, tags);
			storageLister.showAll(showAll);
			storageLister.setAddressFilter(new SiteFilter(null, blacklist));
			return () -> storageLister.iterator(); 
		}
	}

	private StorageClient makeClient(String url) throws Exception {
		Endpoint epr = new Endpoint(url);
		return new StorageClient(epr, 
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
	}

	@Override
	protected String getDetails(StorageClient sms) throws Exception  {
		StringBuilder sb = new StringBuilder();
		sb.append(sms.getEndpoint().getUrl()).append(_newline);
		JSONObject props = sms.getProperties();
		sb.append("  Description: ").append(props.optString("description"));
		long free = -1;
		long use = -1;
		try{
			free = Long.parseLong(String.valueOf(props.get("freeSpace")));
			use  = Long.parseLong(String.valueOf(props.get("usableSpace")));
		}catch(Exception ex) {}
		try{
			sb.append(_newline).append("  Free space:   ").append(unitParser.getHumanReadable(free));
			sb.append(_newline).append("  Usable space: ").append(unitParser.getHumanReadable(use));
			sb.append(_newline).append("  Mount point:  ").append(props.getString("mountPoint"));
			sb.append(_newline).append("  Protocols:    ");
			sb.append(Arrays.asList(JSONUtil.toArray(props.getJSONArray("protocols"))));
			sb.append(_newline).append("  Metadata:     ").append(sms.supportsMetadata()?"yes":"no");
		}catch(Exception ex){}
		return sb.toString();
	}

	@Override
	public String getDescription(){
		return "list the available remote storages";
	}

	@Override
	public String getSynopsis(){
		return "Prints a list  of the available remote storages. "
				+ "With the '-l' option, some additional information is displayed. "
				+ "Use the '-a' option to also list job directories.";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_DATA;
	}

}
