package eu.unicore.ucc.actions.data;

import java.io.OutputStream;

import org.apache.commons.cli.OptionBuilder;
import org.json.JSONObject;

import de.fzj.unicore.uas.fts.FiletransferOptions.SupportsPartialRead;
import de.fzj.unicore.ucc.StorageConstants;
import de.fzj.unicore.ucc.util.ProgressBar;
import eu.unicore.client.Endpoint;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.io.Location;

/**
 * common stuff for file operations
 * @author schuller
 */
public abstract class FileOperation extends ActionBase implements StorageConstants{

	//index of first byte to process
	protected Long startByte;

	//index of last byte to process
	protected Long endByte;

	/**
	 * Preferred protocol, which can be set from the commandline, or from the preferences
	 * file. At least the 'BFT' protocol is guaranteed to work
	 */
	protected String preferredProtocol;

	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_PROTOCOLS_LONG)
				.withDescription("Preferred Protocol")
				.withArgName("Protocol")
				.hasArg()
				.isRequired(false)
				.create(OPT_PROTOCOLS)
				);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_BYTES_LONG)
				.withDescription("Byte range")
				.withArgName("ByteRange")
				.hasArg()
				.isRequired(false)
				.create(OPT_BYTES)
				);
	}

	@Override
	public void process() {
		super.process();
		initPreferredProtocol();
		initRange();
	}

	/**
	 * setup the list of protocols. The first matching one (supported by the 
	 * SMS) will be used. This can be set using a commandline option or 
	 * preferences entry
	 */
	protected void initPreferredProtocol(){
		preferredProtocol = getCommandLine().getOptionValue(OPT_PROTOCOLS);
	}

	protected void initRange(){
		String bytes=getOption(OPT_BYTES_LONG, OPT_BYTES);
		if(bytes==null)return;
		String[]tokens=bytes.split("-");
		try{
			String start=tokens[0];
			String end=tokens[1];
			if(start.length()>0){
				startByte=Long.parseLong(start);
				endByte=Long.MAX_VALUE;
			}
			if(end.length()>0){
				endByte=Long.parseLong(end);
				if(startByte==null){
					startByte=Long.valueOf(0l);
				}
			}
		}catch(Exception e){
			throw new IllegalArgumentException("Could not parse byte range "+bytes);
		}
	}

	protected void runRawTransfer(String url, OutputStream out, ProgressBar p)throws Exception {
		//TODO nicer way to find and configure protocol handlers
		if(url.startsWith("http")){
			JSONObject props = new JSONObject();
			props.put("accessURL", url);
			Endpoint ep = new Endpoint(url);
			HttpFileTransferClient hc = new HttpFileTransferClient(ep, props, configurationProvider.getAnonymousClientConfiguration(), null);
			hc.setProgressListener(p);
			if(startByte!=null){
				verbose("Byte range: "+startByte+"-"+endByte);
				SupportsPartialRead pReader=(SupportsPartialRead)hc;
				pReader.readPartial(startByte, endByte-startByte, out);
			}
			else{
				hc.readAllData(out);
			}
			p.finish();
		}
		else throw new Exception("No protocol handler for "+url);
	}
	
	protected String getEffectiveProtocol(Location ... locations) {
		String selectedProtocol = preferredProtocol;
		for(Location l: locations) {
			if(!l.isLocal() && l.getProtocol()!=null) {
				selectedProtocol = l.getProtocol();
			}
		}
		return selectedProtocol;
	}
}
