package eu.unicore.ucc.actions.info;

import java.util.Arrays;

import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.UnitParser;
import de.fzj.unicore.ucc.UCC;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.lookup.StorageLister;

public class ListStorages extends ListActionBase<StorageClient> {

	public String getName(){
		return "list-storages";
	}

	protected UnitParser unitParser=UnitParser.getCapacitiesParser(1);

	@Override
	public void process() {
		super.process();
		// do we have a list of storages
		if(getCommandLine().getArgList().size()>1){
			boolean first = true;
			for(Object arg : getCommandLine().getArgList()){
				if(first){
					first=false;
					continue;
				}
				try{
					listSMS(makeClient(String.valueOf(arg)));
				}catch(Exception ex){
					error("Error listing storage at "+arg, ex);
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
		for(StorageClient sms: storageLister){
			try {
				if(sms==null){
					if(!storageLister.isRunning()){
						break;
					}
				}
				else{
					if(filterMatch(sms)){
						listSMS(sms);
						lastNumberOfResults++;
					}
				}
			}catch(Exception ex) {
				error("Error listing storage at <"+sms.getEndpoint().getUrl()+">",ex);
			}
		}
	}

	protected void listSMS(StorageClient sms){
		try{
			message(sms.getEndpoint().getUrl()+System.getProperty("line.separator")+getDetails(sms));
			properties.put(PROP_LAST_RESOURCE_URL, sms.getEndpoint().getUrl());
		}catch(Exception ex){
			error("Error listing storage at "+sms.getEndpoint().getUrl(), ex);
		}
		printProperties(sms);
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
		return "General";
	}

}
