package eu.unicore.ucc.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.uas.FiletransferParameterProvider;
import eu.unicore.uas.util.PropertyHelper;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.Constants;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.helpers.ConsoleLogger;
import eu.unicore.ucc.util.JSONUtil;
import eu.unicore.ucc.util.ProgressBar;

/**
 * Copy a file from a remote location to another remote location. 
 * At least one remote must be UNICORE.
 *
 * (if source and target are both UNICORE and the same SMS, the simple 
 * StorageManagement copy operation is used)
 *
 * @author schuller
 */
public class ServerToServer implements Constants {

	private final Location sourceDesc;
	private final Location targetDesc;
	private final UCCConfigurationProvider configurationProvider;

	private String preferredProtocol;

	private TransferControllerClient tcc;

	private boolean synchronous = true;

	// both sides UNICORE?
	private boolean bothSidesUNICORE = true;
	// one side is UNICORE AND is the receiver
	private boolean UNICOREReceives = true;

	protected long remoteSize = -1;

	private String scheduled = null;

	private final ConsoleLogger console = UCC.console;

	private String transferAddress;

	protected Properties extraParameterSource;

	protected Map<String,String> extraParameters;

	public ServerToServer(Location sourceDesc, Location targetDesc, UCCConfigurationProvider configurationProvider){
		this.sourceDesc = sourceDesc;
		this.targetDesc = targetDesc;
		this.configurationProvider = configurationProvider;
		this.preferredProtocol = "BFT";
	}

	public void setExtraParameters(Map<String,String>params){
		this.extraParameters = params;
	}

	public void setExtraParameterSource(Properties properties){
		this.extraParameterSource = properties;
	}

	private Map<String,String> getExtraParameters(String protocol){
		Map<String,String> res = new HashMap<>();
		if(protocol!=null && extraParameterSource!=null){
			String p=String.valueOf(protocol);
			PropertyHelper ph=new PropertyHelper(extraParameterSource, new String[]{p,p.toLowerCase()});
			res.putAll(ph.getFilteredMap());
		}
		ServiceLoader<FiletransferParameterProvider> ppLoader = ServiceLoader.load(FiletransferParameterProvider.class);
		Iterator<FiletransferParameterProvider> ppIter = ppLoader.iterator();
		while(ppIter.hasNext()) {
			FiletransferParameterProvider pp = ppIter.next();
			pp.provideParameters(res, protocol);
		}
		res.putAll(extraParameters);
		if(res.size()>0){
			console.debug("Have <{}> extra parameters for the transfer.", extraParameters.size(), protocol);
		}
		return res;
	}

	public void process() throws Exception {
		if(scheduled!=null){
			scheduled = UnitParser.convertDateToISO8601(scheduled);
			console.debug("Will schedule transfer for {}", scheduled);
			synchronous = false;
		}
		console.debug("Synchronous transfer = {}", synchronous);
		bothSidesUNICORE = !sourceDesc.isRaw && !targetDesc.isRaw();
		if(bothSidesUNICORE  && sourceDesc.getSmsEpr().equalsIgnoreCase(targetDesc.getSmsEpr())) {
			Endpoint target = new Endpoint(targetDesc.getSmsEpr());
			StorageClient sms = new StorageClient(target,
					configurationProvider.getClientConfiguration(target.getUrl()),
					configurationProvider.getRESTAuthN());
			smsCopyFile(sms);
		}
		else {
			copyFile();
		}
	}

	private boolean assertSourceExists(Location remote) throws Exception {
		if(hasWildCards(remote.getName()))return true;
		StorageClient source = new StorageClient(new Endpoint(remote.getSmsEpr()),
				configurationProvider.getClientConfiguration(remote.getSmsEpr()),
				configurationProvider.getRESTAuthN());
		FileListEntry fle = source.stat(remote.getName());
		remoteSize = fle.size;
		return true;
	}

	private boolean hasWildCards(String name){
		return name.contains("*") || name.contains("?");
	}

	/**
	 * perform the remote copy, and cleanup the file transfer resource (if not in async mode)
	 */
	private void copyFile() throws Exception {
		if(!sourceDesc.isRaw())assertSourceExists(sourceDesc);
		UNICOREReceives = sourceDesc.isRaw();
		try {
			if(bothSidesUNICORE || UNICOREReceives) {
				Endpoint target = new Endpoint(targetDesc.getSmsEpr());
				StorageClient sms = new StorageClient(target,
						configurationProvider.getClientConfiguration(target.getUrl()),
						configurationProvider.getRESTAuthN());
				String protocol = bothSidesUNICORE? checkProtocols(sms):null;
				String s = sourceDesc.isRaw()?sourceDesc.originalDescriptor:sourceDesc.getUnicoreURI();
				console.verbose("Initiating fetch-file on storage <{}>, receiving file <{}>, writing to '{}'",
						sms.getEndpoint().getUrl(), s, targetDesc.getName());
				Map<String,String> params = getExtraParameters(protocol);
				tcc = sms.fetchFile(sourceDesc.getResolvedURL(), targetDesc.getName(), params, protocol);
			}
			else {
				// source sends
				Endpoint source = new Endpoint(sourceDesc.getSmsEpr());
				StorageClient sms = new StorageClient(source,
						configurationProvider.getClientConfiguration(source.getUrl()),
						configurationProvider.getRESTAuthN());
				console.verbose("Initiating send-file on storage <{}>, sending file <{}>, writing to '{}'",
						sms.getEndpoint().getUrl(),	sourceDesc.getName(), targetDesc.getResolvedURL());
				Map<String,String> params = getExtraParameters(null);
				tcc = sms.sendFile(sourceDesc.getName(), targetDesc.getResolvedURL(), params, null);
			}
			transferAddress = tcc.getEndpoint().getUrl();
			console.debug("Have filetransfer instance: {}", transferAddress);
			if(synchronous) {
				console.verbose("Waiting for transfer to complete...");
				waitForCompletion();
				console.verbose("Transfer done.");
			}
		} finally{
			if(synchronous && tcc!=null){
				try{ tcc.delete(); }
				catch(Exception e1){}
			}
		}
	}

	/**
	 * wait until complete or failed
	 * @throws Exception
	 */
	private void waitForCompletion()throws Exception{
		long transferred=-1;
		ProgressBar p=new ProgressBar(sourceDesc.getName(),remoteSize);
		tcc.setUpdateInterval(-1);
		String status = "UNDEFINED";
		do{
			JSONObject props = tcc.getProperties();
			Thread.sleep(1000);
			transferred = props.getLong("transferredBytes");
			p.updateTotal(transferred);
			status = props.getString("status");
		}while(!"FAILED".equals(status) && !"DONE".equals(status));
		p.finish();
		if("FAILED".equals(status)){
			String desc = tcc.getProperties().getString("statusMessage");
			throw new Exception("File transfer FAILED: "+desc);
		}
	}

	private void smsCopyFile(StorageClient sms) throws Exception {
		console.verbose("Copy on remote storage: {}->{}", sourceDesc.getName(), targetDesc.getName());
		JSONObject params = new JSONObject();
		params.put("from", sourceDesc.getName());
		params.put("to", targetDesc.getName());
		sms.executeAction("copy", params);
	}

	private String checkProtocols(StorageClient sms)throws Exception{
		if(!"BFT".equals(preferredProtocol)){
			List<String> supported = JSONUtil.asList(sms.getProperties().getJSONArray("protocols"));
			if(supported.contains(preferredProtocol)) {
				console.debug("Using preferred protocol: {}", preferredProtocol);
				return preferredProtocol;
			}
		}
		return null;
	}

	public void setPreferredProtocol(String preferredProtocol){
		this.preferredProtocol = preferredProtocol;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	public void setScheduled(String scheduled) {
		this.scheduled = scheduled;
	}

	public String getTransferAddress() {
		return transferAddress;
	}

}
