package eu.unicore.ucc.io;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.uas.util.PropertyHelper;
import de.fzj.unicore.uas.util.UnitParser;
import de.fzj.unicore.ucc.Constants;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.helpers.EndProcessingException;
import de.fzj.unicore.ucc.util.JSONUtil;
import de.fzj.unicore.ucc.util.ProgressBar;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.services.rest.client.BaseClient;

/**
 * Copy a file from a remote sms to another remote SMS <br/>
 *
 * (if source and target are the same SMS, the simple StorageManagement copy operation is used)
 * 
 * @author schuller
 */
public class ServerToServer implements Constants {

	private final Location sourceDesc;
	private final Location targetDesc;
	private final UCCConfigurationProvider configurationProvider;

	/**
	 * output directory for async transfer descriptor
	 */
	protected File output = new File(".");

	private String preferredProtocol;

	protected BaseServiceClient tcc;

	protected StorageClient sms;

	private boolean synchronous = true;

	protected long remoteSize=-1;

	private String scheduled=null;

	private MessageWriter msg;

	private String transferAddress;

	protected Properties extraParameterSource;

	public ServerToServer(Location sourceDesc, Location targetDesc, UCCConfigurationProvider configurationProvider){
		this.sourceDesc = sourceDesc;
		this.targetDesc = targetDesc;
		this.configurationProvider = configurationProvider;
		this.preferredProtocol = "BFT";
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
		
		Endpoint target = new Endpoint(targetDesc.getSmsEpr());

		sms = new StorageClient(target,
				configurationProvider.getClientConfiguration(target.getUrl()),
				configurationProvider.getRESTAuthN());
		if(sourceDesc.getSmsEpr().equalsIgnoreCase(targetDesc.getSmsEpr())){
			smsCopyFile();
		}
		else copyFile();
	}
	
	protected boolean assertRemoteExists(Location remote) throws Exception {
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
	protected void copyFile(){
		try{
			checkProtocols();
			assertRemoteExists(sourceDesc);
			msg.verbose("Initiating fetch-file on storage = <"+sms.getEndpoint().getUrl()+">," +
					"receiving file <"+sourceDesc.getUnicoreURI()+">, writing to "+targetDesc.getName());
			JSONObject params = new JSONObject();
			params.put("file", targetDesc.getName());
			params.put("source", sourceDesc.getUnicoreURI());
			params.put("extraParameters", getExtraParams());
			// TODO method on StorageClient and transfer controller client are missing
			BaseClient bc = new BaseClient(sms.getEndpoint().getUrl()+"/transfers", sms.getSecurityConfiguration(), sms.getAuth());
			HttpResponse res = bc.post(params);
			bc.checkError(res);
			String loc = res.getFirstHeader("Location").getValue();
			tcc = new BaseServiceClient(new Endpoint(loc), sms.getSecurityConfiguration(), sms.getAuth());
			transferAddress = tcc.getEndpoint().getUrl();
			msg.verbose("Have filetransfer instance: "+transferAddress);
			if(!synchronous){
				writeIDFile();
				return;
			}
			waitForCompletion();
		}catch(Exception e){
			msg.error("Can't copy file.",e);
			throw new EndProcessingException(ERROR_CLIENT);
		}
		finally{
			if(synchronous){
				try{ tcc.delete(); }
				catch(Exception e1){
					msg.error("Could not destroy the filetransfer client",e1);
				}
			}
		}
	}

	protected Map<String,String>getExtraParams(){
		Map<String, String>params = new HashMap<>();
		if(scheduled!=null){
			params.put("scheduledStartTime",scheduled);
		}
		String protocol = sourceDesc.getProtocol();
		
		if(extraParameterSource!=null){
			Map<String, String> res = new HashMap<String, String>();
			String p = String.valueOf(protocol);
			PropertyHelper ph = new PropertyHelper(extraParameterSource, new String[]{p,p.toLowerCase()});
			res = ph.getFilteredMap();
			if(res.size()>0){
				msg.verbose("Have "+res.size()+" extra parameters for protocol "+protocol);
				params.putAll(res);
			}
			
			// TODO check if required
			if(res.containsKey("uftp.streams")){
				params.put("streams", res.get("uftp.streams"));
			}
			if(res.containsKey("uftp.encryption")){
				params.put("encryption", res.get("uftp.encryption"));
			}
		}
		return params;
	}

	/**
	 * wait until complete or failed
	 * @throws Exception
	 */
	protected void waitForCompletion()throws Exception{
		long transferred=-1;
		ProgressBar p=new ProgressBar(sourceDesc.getName(),remoteSize,msg);
		tcc.setUpdateInterval(-1);
		String status = "UNDEFINED";
		do{
			JSONObject props = tcc.getProperties();
			Thread.sleep(1000);
			transferred = Long.valueOf(props.getString("transferredBytes"));
			p.updateTotal(transferred);
			status = props.getString("status");
		}while(!"FAILED".equals(status) && !"DONE".equals(status));
		p.finish();
		if("FAILED".equals(status)){
			String desc = tcc.getProperties().getString("statusMessage");
			throw new Exception("File transfer FAILED: "+desc);
		}
	}

	protected void smsCopyFile(){
		try{
			msg.verbose("Copy on remote storage: "+sourceDesc.getName()+"->"+targetDesc.getName());
			JSONObject params = new JSONObject();
			params.put("from", sourceDesc.getName());
			params.put("to", targetDesc.getName());
			sms.executeAction("copy", params);
		} catch(Exception e){
			msg.error("Can't copy file.",e);
			throw new EndProcessingException(ERROR);
		}
	}


	protected void writeIDFile(){
		try{
			String url = tcc.getEndpoint().getUrl();
			File dump=new File(output, JSONUtil.extractResourceID(url)+".transfer");
			FileWriter fw=new FileWriter(dump);
			fw.append(url).append("\n");
			fw.close();
			msg.verbose("Wrote transfer descriptor to "+dump.getAbsolutePath());
			msg.message(dump.getAbsolutePath());
		}
		catch(Exception e){
			msg.error("Could not write job ID file.",e);
		}
	}

	protected void checkProtocols()throws Exception{
		if(!"BFT".equals(preferredProtocol)){
			List<String> supported = JSONUtil.asList(sms.getProperties().getJSONArray("protocols"));
			if(supported.contains(preferredProtocol)) {
				sourceDesc.setProtocol(preferredProtocol);
				msg.verbose("Using preferred protocol: "+preferredProtocol);
				return;
			}
		}
	}

	public void setPreferredProtocol(String preferredProtocol){
		this.preferredProtocol = preferredProtocol;
	}

	public File getOutput() {
		return output;
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public boolean isSynchronous() {
		return synchronous;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	public String getScheduled() {
		return scheduled;
	}

	public void setScheduled(String scheduled) {
		this.scheduled = scheduled;
	}

	public String getPreferredProtocol() {
		return preferredProtocol;
	}

	public long getRemoteSize() {
		return remoteSize;
	}

	public void setMessageWriter(MessageWriter msg) {
		this.msg = msg;
	}
	
	public String getTransferAddress() {
		return transferAddress;
	}

}
