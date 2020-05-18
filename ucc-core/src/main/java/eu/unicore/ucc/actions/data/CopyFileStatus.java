package eu.unicore.ucc.actions.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;

import de.fzj.unicore.uas.util.UnitParser;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.client.data.TransferControllerClient.Status;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.io.Location;

/**
 * check the status of a asynchronous copy-file operation (@see CopyFile})
 */
public class CopyFileStatus extends ActionBase {

	protected Location sourceDesc;
	protected Location targetDesc;
	protected TransferControllerClient tcc;
	
	protected StorageClient sms;
	
	protected boolean synchronous;
	protected UnitParser up;
	
	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
	}

	@Override
	public void process() {
		super.process();
		try{
			up=UnitParser.getCapacitiesParser(3);
			initTCC();
			verbose("Accessing transfer at "+tcc.getEndpoint().getUrl());
			doCheck();
		}catch(Exception e){
			error("Can't check status.",e);
			endProcessing(ERROR);
		}
	}
	
	protected void initTCC()throws Exception{
		String arg=null;
		if(getCommandLine().getArgs().length>0)arg=getCommandLine().getArgs()[1];
		Endpoint epr=null;
		if(arg==null){
			//read from stdin
			try{
				String url=new BufferedReader(new InputStreamReader(System.in)).readLine();
				epr = new Endpoint(url);
			}catch(Exception e){
				System.err.println("Can't read file transfer descriptor from stdin.");
				endProcessing();
			}
		}
		else{
			if(new File(arg).exists()){
				String url = FileUtils.readFileToString(new File(arg), "UTF-8").trim();
				epr = new Endpoint(url);
			}
			else{
				epr = new Endpoint(arg);
			}
		}
		tcc = new TransferControllerClient(epr,
				configurationProvider.getClientConfiguration(epr.getUrl()),
				configurationProvider.getRESTAuthN());
	}
	
	protected void doCheck()throws Exception{
		Long transferred = tcc.getTransferredBytes();
		Status status = tcc.getStatus();
		message(status+", <"+up.getHumanReadable(transferred)+"> bytes transferred");
	}
	
	@Override
	public String getName() {
		return "copy-file-status";
	}

	@Override
	public String getSynopsis() {
		return "Checks the status of a server-to-server transfer.";
	}
	
	@Override
	public String getDescription(){
		return "check status of a server-to-server transfer";
	}
	
	@Override
	public String getArgumentList(){
		return "[IdFile|Address]";
	}
	@Override
	public String getCommandGroup(){
		return "Data management";
	}
}
