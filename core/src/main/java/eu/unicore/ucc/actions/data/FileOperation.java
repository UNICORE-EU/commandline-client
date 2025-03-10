package eu.unicore.ucc.actions.data;

import java.io.OutputStream;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.uas.fts.FiletransferOptions.SupportsPartialRead;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.StorageConstants;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.io.Location;
import eu.unicore.ucc.util.ProgressBar;

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
	 * file. At least the 'BFT' protocol is always available.
	 */
	protected String preferredProtocol;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_PROTOCOLS)
				.longOpt(OPT_PROTOCOLS_LONG)
				.desc("Preferred Protocol")
				.argName("Protocol")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_BYTES)
				.longOpt(OPT_BYTES_LONG)
				.desc("Byte range")
				.argName("ByteRange")
				.hasArg()
				.required(false)
				.build());
	}

	@Override
	public void process() throws Exception {
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
			if(tokens.length>1) {
				String start=tokens[0];
				String end=tokens[1];
				if(start.length()>0){
					startByte = (long)(UnitParser.getCapacitiesParser(0).getDoubleValue(start));
					endByte=Long.MAX_VALUE;
				}
				if(end.length()>0){
					endByte = (long)(UnitParser.getCapacitiesParser(0).getDoubleValue(end));
					if(startByte==null){
						startByte=Long.valueOf(0l);
					}
				}
			}
			else {
				String end=tokens[0];
				endByte = (long)(UnitParser.getCapacitiesParser(0).getDoubleValue(end));
				startByte = Long.valueOf(0l);
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
				console.verbose("Byte range: {}-{}", startByte, endByte);
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

	@Override
	public String getCommandGroup(){
		return CMD_GRP_DATA;
	}

}
