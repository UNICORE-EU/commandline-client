package eu.unicore.ucc.io;

import java.util.List;
import java.util.Properties;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.TransferControllerClient;
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

	protected TransferControllerClient tcc;

	private boolean synchronous = true;

	// both sides UNICORE?
	private boolean bothSidesUNICORE = true;
	// one side is UNICORE AND is the receiver
	private boolean UNICOREReceives = true;

	protected long remoteSize=-1;

	private String scheduled=null;

	private final ConsoleLogger msg;

	private String transferAddress;

	protected Properties extraParameterSource;

	public ServerToServer(Location sourceDesc, Location targetDesc, UCCConfigurationProvider configurationProvider){
		this.sourceDesc = sourceDesc;
		this.targetDesc = targetDesc;
		this.configurationProvider = configurationProvider;
		this.preferredProtocol = "BFT";
		this.msg = UCC.console;
	}

	public void setExtraParameterSource(Properties properties){
		this.extraParameterSource = properties;
	}

	public void process() throws Exception {
		if(scheduled!=null){
			scheduled=UnitParser.convertDateToISO8601(scheduled);
			msg.verbose("Will schedule transfer for "+scheduled);
			synchronous = false;
		}
		msg.verbose("Synchronous transfer = "+synchronous);

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

	protected boolean assertSourceExists(Location remote) throws Exception {
		if(hasWildCards(remote.getName()))return true;
		StorageClient source = new StorageClient(new Endpoint(remote.getSmsEpr()),
				configurationProvider.getClientConfiguration(remote.getSmsEpr()),
				configurationProvider.getRESTAuthN());
		FileListEntry fle = source.stat(remote.getName());
		remoteSize = fle.size;
		return true;
	}

	public boolean hasWildCards(String name){
		return name.contains("*") || name.contains("?");
	}

	/**
	 * perform the remote copy, and cleanup the file transfer resource (if not in async mode)
	 */
	protected void copyFile() throws Exception {
		if(!sourceDesc.isRaw())assertSourceExists(sourceDesc);
		UNICOREReceives = sourceDesc.isRaw();
		try {
			if(bothSidesUNICORE || UNICOREReceives) {
				Endpoint target = new Endpoint(targetDesc.getSmsEpr());
				StorageClient sms = new StorageClient(target,
						configurationProvider.getClientConfiguration(target.getUrl()),
						configurationProvider.getRESTAuthN());
				String protocol = bothSidesUNICORE? checkProtocols(sms):null;
				msg.verbose("Initiating fetch-file on storage <"+sms.getEndpoint().getUrl()+">," +
						"receiving file <"+sourceDesc.getUnicoreURI()+">, writing to '"+targetDesc.getName()+"'");
				tcc = sms.fetchFile(sourceDesc.getResolvedURL(), targetDesc.getName(), protocol);
			}
			else {
				// source sends
				Endpoint source = new Endpoint(sourceDesc.getSmsEpr());
				StorageClient sms = new StorageClient(source,
						configurationProvider.getClientConfiguration(source.getUrl()),
						configurationProvider.getRESTAuthN());
				msg.verbose("Initiating send-file on storage <{}>, sending file <{}>, writing to '{}'",
						sms.getEndpoint().getUrl(),	sourceDesc.getName(), targetDesc.getResolvedURL());
				tcc = sms.sendFile(sourceDesc.getName(), targetDesc.getResolvedURL(), null);
			}
			transferAddress = tcc.getEndpoint().getUrl();
			msg.verbose("Have filetransfer instance: {}", transferAddress);
			if(!synchronous){
				return;
			}
			waitForCompletion();
		} finally{
			if(synchronous && tcc!=null){
				try{ tcc.delete(); }
				catch(Exception e1){
					msg.error(e1, "Could not destroy the filetransfer client");
				}
			}
		}
	}

	/**
	 * wait until complete or failed
	 * @throws Exception
	 */
	protected void waitForCompletion()throws Exception{
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

	protected void smsCopyFile(StorageClient sms) throws Exception {
		msg.verbose("Copy on remote storage: "+sourceDesc.getName()+"->"+targetDesc.getName());
		JSONObject params = new JSONObject();
		params.put("from", sourceDesc.getName());
		params.put("to", targetDesc.getName());
		sms.executeAction("copy", params);
	}

	protected String checkProtocols(StorageClient sms)throws Exception{
		if(!"BFT".equals(preferredProtocol)){
			List<String> supported = JSONUtil.asList(sms.getProperties().getJSONArray("protocols"));
			if(supported.contains(preferredProtocol)) {
				msg.verbose("Using preferred protocol: "+preferredProtocol);
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
