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

	public String getName(){
		return "list-storages";
	}

	protected UnitParser unitParser=UnitParser.getCapacitiesParser(1);

	private boolean showAll = false;

	@Override
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(Option.builder(OPT_ALL)
				.longOpt(OPT_ALL_LONG)
				.desc("Show all storages including job directories")
				.required(false)
				.build());
	}

	@Override
	public void process() throws Exception {
		super.process();
		this.showAll = getCommandLine().hasOption(OPT_ALL);
		console.verbose("Listing job directories = {}", showAll);
		// do we have a list of storages
		if(getCommandLine().getArgList().size()>1){
			boolean first = true;
			for(Object arg : getCommandLine().getArgList()){
				if(first){
					first=false;
					continue;
				}
				try{
					listStorage(makeClient(String.valueOf(arg)));
				}catch(Exception ex){
					console.error(ex, "Error listing storage at {}", arg);
				}
			}
		}
		else{
			listAll();
		}
	}
	
	protected StorageClient makeClient(String url) throws Exception {
		Endpoint epr = new Endpoint(url);
		properties.put(PROP_LAST_RESOURCE_URL, url);
		return new StorageClient(epr, 
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
	}
	
	protected void listAll(){
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

	protected void listStorage(StorageClient storage) throws Exception {
		String url = storage.getEndpoint().getUrl();
		try{
			console.info("{}{}{}", url, System.getProperty("line.separator"), getDetails(storage));
			properties.put(PROP_LAST_RESOURCE_URL, url);
			URLCompleter.registerSiteURL(url);
		}catch(Exception ex){
			console.error(ex, "Error listing storage at {}", url);
		}
		printProperties(storage);
	}

	@Override
	protected String getDetails(StorageClient sms) throws Exception  {
		return detailed? doGetDetails(sms) : "";
	}
	
	protected String doGetDetails(StorageClient sms) throws Exception {
		String sep=System.getProperty("line.separator");
		StringBuilder sb=new StringBuilder();
		JSONObject props = sms.getProperties();
		
		sb.append("  Description: ").append(props.optString("description"));
		
		long free = -1;
		long use = -1;
		try{
			free = Long.parseLong(props.getString("freeSpace"));
			use = Long.parseLong(props.getString("usableSpace"));
		}catch(Exception ex) {}
		try{
			sb.append(sep).append("  Free space:   ").append(unitParser.getHumanReadable(free));
			sb.append(sep).append("  Usable space: ").append(unitParser.getHumanReadable(use));
			sb.append(sep).append("  Mount point:  ").append(props.getString("mountPoint"));
			sb.append(sep).append("  Protocols:    ");
			sb.append(Arrays.asList(JSONUtil.toArray(props.getJSONArray("protocols"))));
			sb.append(sep).append("  Metadata:     ").append(sms.supportsMetadata()?"yes":"no");
		}catch(Exception ex){}
		return sb.toString();
	}

	@Override
	public String getDescription(){
		return "list the available remote storages";
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
