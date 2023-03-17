package eu.unicore.ucc.actions.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Option;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.io.FileDownloader;
import eu.unicore.ucc.io.FileTransferBase;
import eu.unicore.ucc.io.FileTransferBase.Mode;
import eu.unicore.ucc.util.ProgressBar;
import eu.unicore.ucc.io.FileUploader;
import eu.unicore.ucc.io.Location;
import eu.unicore.ucc.io.ServerToServer;

/**
 * Copy multiple file(s) to a destination<br/>
 * Both source and target can be local and/or remote
 * 
 * @author schuller
 */
public class CP extends FileOperation {

	protected String target;
	protected final List<String>sources = new ArrayList<>();
	
	protected boolean append;
	protected boolean recurse;
	protected boolean resume;
	
	@Override
	protected void createOptions() {
		super.createOptions();
		
		getOptions().addOption(Option.builder(OPT_MODE)
				.longOpt(OPT_MODE_LONG)
				.desc("(server-server only) Asynchronous mode, writes the transfer ID to a file.")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_SCHEDULED)
				.longOpt(OPT_SCHEDULED_LONG)
				.desc("(server-server only) Schedule the transfer for a specific time (in HH:mm or ISO8601 format)")
				.required(false)
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_RESUME)
				.longOpt(OPT_RESUME_LONG)
				.desc("(client-server only) Resume previous transfer, appending only missing data.")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_RECURSIVE)
				.longOpt(OPT_RECURSIVE_LONG)
				.desc("Recurse into subdirectories")
				.required(false)
				.build());
	}

	@Override
	public void process() {
		super.process();
		String[]args = getCommandLine().getArgs();
		if(args.length<3)throw new IllegalArgumentException("Must have source and target arguments!");
		target = args[args.length-1];
		for(int i=1; i<args.length-1; i++){
			sources.add(args[i]);
		}
		Location targetDesc = createLocation(target);
		recurse = getBooleanOption(OPT_RECURSIVE_LONG, OPT_RECURSIVE);
		if(recurse)verbose("Recurse into subdirectories="+recurse);
		resume = getBooleanOption(OPT_RESUME_LONG, OPT_RESUME);
		if(resume)verbose("Resume previous transfer(s)="+resume);
		String scheduled = getOption(OPT_SCHEDULED_LONG, OPT_SCHEDULED);
		boolean synchronous = !getBooleanOption(OPT_MODE_LONG, OPT_MODE);

		try{
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
		}catch(Exception e){
			error("Can't copy file(s).",e);
			endProcessing(ERROR);
		}
	}

	protected void handleServerServer(Location sourceDesc, Location targetDesc, String scheduled, boolean synchronous) 
	throws Exception {
		if(sourceDesc.isRaw() && targetDesc.isRaw()) {
			throw new IllegalArgumentException("One of source or target must be a UNICORE storage!");
		}
		ServerToServer transfer = new ServerToServer(sourceDesc, targetDesc, configurationProvider);
		transfer.setMessageWriter(this);
		transfer.setScheduled(scheduled);
		transfer.setSynchronous(synchronous);
		transfer.setPreferredProtocol(preferredProtocol);
		transfer.setExtraParameterSource(properties);
		transfer.process();
		if(transfer.getTransferAddress()!=null) {
			properties.put(PROP_LAST_RESOURCE_URL, transfer.getTransferAddress());
		}
		lastTransferAddress = transfer.getTransferAddress();
	}
	
	protected void handleClientServer(String source, Location sourceDesc, Location targetDesc) 
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
		fd.setStartByte(startByte);
		fd.setEndByte(endByte);
		fd.setPreferredProtocol(selectedProtocol);
		fd.setRecurse(recurse);
		fd.setExtraParameterSource(properties);
		StorageClient sms=new StorageClient(new Endpoint(url),
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
		fd.perform(sms, this);
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
	@Override
	public String getCommandGroup(){
		return "Data management";
	}
	
	// for unit-testing
	public static String lastTransferAddress;
	
	private void rawDownload(String url)throws Exception {
		File tFile = new File(target);
		try(OutputStream os=new FileOutputStream(tFile)){
			runRawTransfer(url, os, new ProgressBar(tFile.getName(),-1,this));
		}
	}
	
}
