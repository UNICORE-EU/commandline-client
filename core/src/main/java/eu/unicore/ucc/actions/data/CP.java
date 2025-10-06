package eu.unicore.ucc.actions.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.io.FileDownloader;
import eu.unicore.ucc.io.FileTransferBase;
import eu.unicore.ucc.io.FileTransferBase.Mode;
import eu.unicore.ucc.io.FileUploader;
import eu.unicore.ucc.io.Location;
import eu.unicore.ucc.io.ServerToServer;
import eu.unicore.ucc.util.ProgressBar;

/**
 * Copy multiple file(s) to a destination<br/>
 * Both source and target can be local and/or remote
 * 
 * @author schuller
 */
public class CP extends FileOperation {

	private String target;
	private final List<String>sources = new ArrayList<>();

	private boolean recurse;
	private boolean resume;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_MODE)
				.longOpt(OPT_MODE_LONG)
				.desc("(server-server only) Asynchronous mode, writes the transfer ID to a file.")
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_SCHEDULED)
				.longOpt(OPT_SCHEDULED_LONG)
				.desc("(server-server only) Schedule the transfer for a specific time (in HH:mm or ISO8601 format)")
				.required(false)
				.hasArg()
				.get());
		getOptions().addOption(Option.builder(OPT_RESUME)
				.longOpt(OPT_RESUME_LONG)
				.desc("(client-server only) Resume previous transfer, appending only missing data.")
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_RECURSIVE)
				.longOpt(OPT_RECURSIVE_LONG)
				.desc("Recurse into subdirectories")
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_EXTRA_PARAMETERS)
				.longOpt(OPT_EXTRA_PARAMETERS_LONG)
				.desc("Additional settings for the transfer (key1=val1,key2=val2).")
				.required(false)
				.hasArg()
				.get());
	}

	@Override
	public void process() throws Exception {
		super.process();
		String[]args = getCommandLine().getArgs();
		if(args.length<3)throw new IllegalArgumentException("Must have source and target arguments!");
		target = args[args.length-1];
		for(int i=1; i<args.length-1; i++){
			sources.add(args[i]);
		}
		Location targetDesc = createLocation(target);
		recurse = getBooleanOption(OPT_RECURSIVE_LONG, OPT_RECURSIVE);
		if(recurse)console.debug("Recurse into subdirectories = {}", recurse);
		resume = getBooleanOption(OPT_RESUME_LONG, OPT_RESUME);
		if(resume)console.debug("Resume previous transfer(s) = {}", resume);
		String scheduled = getOption(OPT_SCHEDULED_LONG, OPT_SCHEDULED);
		boolean synchronous = !getBooleanOption(OPT_MODE_LONG, OPT_MODE);
		for(String source: sources){
			Location sourceDesc = createLocation(source);
			boolean isServerToServer = !sourceDesc.isLocal() && !targetDesc.isLocal();
			if(isServerToServer){
				handleServerServer(sourceDesc, targetDesc, scheduled, synchronous);
			}
			else{
				if(sourceDesc.isLocal() && targetDesc.isLocal()) {
					throw new IllegalArgumentException("One of source or target must be remote!");
				}
				handleClientServer(source, sourceDesc, targetDesc);
			}
		}
	}

	private void handleServerServer(Location sourceDesc, Location targetDesc, String scheduled, boolean synchronous) 
	throws Exception {
		if(sourceDesc.isRaw() && targetDesc.isRaw()) {
			throw new IllegalArgumentException("One of source or target must be a UNICORE storage!");
		}
		ServerToServer transfer = new ServerToServer(sourceDesc, targetDesc, configurationProvider);
		transfer.setScheduled(scheduled);
		transfer.setSynchronous(synchronous);
		transfer.setPreferredProtocol(preferredProtocol);
		transfer.setExtraParameters(getExtraParameters());
		transfer.setExtraParameterSource(properties);
		transfer.process();
		if(transfer.getTransferAddress()!=null) {
			properties.put(PROP_LAST_RESOURCE_URL, transfer.getTransferAddress());
		}
		lastTransferAddress = transfer.getTransferAddress();
	}
	
	private void handleClientServer(String source, Location sourceDesc, Location targetDesc) 
	throws Exception {
		boolean isDownload = targetDesc.isLocal();
		FileTransferBase fd = null;
		Mode mode = resume ? Mode.RESUME : Mode.NORMAL;
		String url;
		String selectedProtocol = getEffectiveProtocol(sourceDesc, targetDesc);
		if(isDownload){
			if(sourceDesc.isRaw()){
				rawDownload(sourceDesc.getSmsEpr());
				return;
			}
			String from = sourceDesc.getName();
			url = sourceDesc.getSmsEpr();
			fd = new FileDownloader(from, target, mode);
		}
		else{
			String to = targetDesc.getName();
			url = targetDesc.getSmsEpr();
			fd = new FileUploader(new File("."), source, to, mode);
		}
		StorageClient sms=new StorageClient(new Endpoint(url),
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
		fd.setStorageClient(sms);
		fd.setStartByte(startByte);
		fd.setEndByte(endByte);
		fd.setPreferredProtocol(selectedProtocol);
		fd.setRecurse(recurse);
		fd.setExtraParameters(getExtraParameters());
		fd.setExtraParameterSource(properties);
		fd.callAndCheck();
	}
	
	@Override
	public String getName() {
		return "cp";
	}

	@Override
	public String getSynopsis() {
		return "Copies files.";
	}
	@Override
	public String getDescription(){
		return "copy source file(s) to a target destination.";
	}

	@Override
	public String getArgumentList(){
		return "<sources(s)> <target>";
	}

	private Map<String,String>getExtraParameters(){
		Map<String,String> params = new HashMap<>();
		if(getCommandLine().hasOption(OPT_EXTRA_PARAMETERS)) {
			try{
				String ep = getOption(OPT_EXTRA_PARAMETERS_LONG, OPT_EXTRA_PARAMETERS).trim();
				String[] tok = ep.split(",");
				for(String t: tok) {
					String[]kv = t.split("=",2);
					if(kv.length==2) {
						params.put(kv[0], kv[1]);
					}
				}
			}catch(Exception e) {
				throw new IllegalArgumentException("Cannot parse extra parameters!");
			}
		}
		return params;
	}

	// for unit-testing
	public static String lastTransferAddress;

	private void rawDownload(String url)throws Exception {
		File tFile = new File(target);
		try(OutputStream os=new FileOutputStream(tFile)){
			runRawTransfer(url, os, new ProgressBar(tFile.getName(),-1));
		}
	}
}
