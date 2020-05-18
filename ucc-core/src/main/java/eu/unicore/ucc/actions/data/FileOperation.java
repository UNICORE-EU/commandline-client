package eu.unicore.ucc.actions.data;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.OptionBuilder;

import de.fzj.unicore.uas.client.HttpBasicClient;
import de.fzj.unicore.uas.fts.FiletransferOptions.SupportsPartialRead;
import de.fzj.unicore.ucc.StorageConstants;
import de.fzj.unicore.ucc.util.ProgressBar;
import eu.unicore.ucc.actions.ActionBase;

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
	 * List of preferred protocols, which can be set from the commandline, or from the preferences
	 * file. At least the BFT protocol is guaranteed to be in this list.
	 */
	protected final List<String> preferredProtocols = new ArrayList<>();

	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_PROTOCOLS_LONG)
				.withDescription("Preferred Protocols")
				.withArgName("ProtocolList")
				.hasArgs()
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
		initPreferredProtocols();
		initRange();
	}

	/**
	 * setup the list of protocols. The first matching one (supported by the 
	 * SMS) will be used. This can be set using a commandline option or 
	 * preferences entry
	 */
	protected void initPreferredProtocols(){
		String[] protocols=getCommandLine().getOptionValues(OPT_PROTOCOLS);
		preferredProtocols.clear();
		preferredProtocols.addAll(getPreferredProtocols(protocols, properties));
	}

	/**
	 * create a list of preferred protocols from the given space separated string.
	 * If this is empty, the properties are checked for a "protocols" property
	 * 
	 * @param protocols
	 * @param properties
	 * @return list of preferred protocols
	 */
	public static List<String>getPreferredProtocols(String[] protocols, Properties properties)throws IllegalArgumentException{
		List<String> result=new ArrayList<>();
		if(protocols==null || protocols.length==0){
			if(properties!=null){
				//get from preferences
				String protoP=properties.getProperty(OPT_PROTOCOLS_LONG,"BFT");
				protocols=protoP.split(" +");
			}
			if(protocols==null || protocols.length==0){
				protocols=new String[]{"BFT"};
			}
		}
		for(String p: protocols){
			if(p!=null){
				result.add(p);
			}
		}
		if(!result.contains("BFT")){
			result.add("BFT");
		}
		return result;
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
			HttpBasicClient hc=new HttpBasicClient(url, configurationProvider.getAnonymousClientConfiguration());
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

}
