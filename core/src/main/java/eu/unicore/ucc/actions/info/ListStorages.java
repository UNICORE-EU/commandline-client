package eu.unicore.ucc.actions.info;

import java.util.Arrays;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.shell.URLCompleter;
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
	public void process() throws Exception {
		super.process();
		this.showAll = getCommandLine().hasOption(OPT_ALL);
		console.debug("Listing job directories = {}", showAll);
		// do we have a list of storages
		if(getCommandLine().getArgList().size()>1){
			getCommandLine().getArgList().listIterator(1).forEachRemaining(
				(url)-> {
					try{
						listStorage(makeClient(String.valueOf(url)));
					}catch(Exception ex){
						console.error(ex, "Error listing storage at {}", url);
					}
				});
		}
		else{
			listAll();
		}
	}

	private StorageClient makeClient(String url) throws Exception {
		Endpoint epr = new Endpoint(url);
		properties.put(PROP_LAST_RESOURCE_URL, url);
		return new StorageClient(epr, 
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
	}

	private void listAll(){
		StorageLister storageLister=new StorageLister(UCC.executor, registry, configurationProvider, tags);
		storageLister.showAll(showAll);
		for(StorageClient sms: storageLister){
			try {
				if(sms==null){
					if(!storageLister.isRunning()){
						break;
					}
				}
				else{
					if(filterMatch(sms)){
						listStorage(sms);
						lastNumberOfResults++;
					}
				}
			}catch(Exception ex) {
				console.error(ex, "Error listing storage at <{}>", sms.getEndpoint().getUrl());
			}
		}
	}

	private void listStorage(StorageClient storage) throws Exception {
		String url = storage.getEndpoint().getUrl();
		try{
			console.info("{}{}{}", url, _newline, getDetails(storage));
			properties.put(PROP_LAST_RESOURCE_URL, url);
			URLCompleter.registerSiteURL(url);
		}catch(Exception ex){
			console.error(ex, "Error listing storage at {}", url);
		}
		printProperties(storage);
	}

	@Override
	protected String getDetails(StorageClient sms) throws Exception  {
		if(!detailed)return "";
		StringBuilder sb = new StringBuilder();
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
