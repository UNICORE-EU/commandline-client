package eu.unicore.ucc.actions.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.uas.util.UnitParser;
import eu.unicore.client.Endpoint;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.client.data.TransferControllerClient.Status;
import eu.unicore.ucc.actions.ActionBase;

/**
 * check the status of asynchronous copy-file operations)
 */
public class CopyFileStatus extends ActionBase {

	protected UnitParser up = UnitParser.getCapacitiesParser(3);

	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
	}

	@Override
	public void process() {
		super.process();
		List<String> urls = new ArrayList<>();
		if(getCommandLine().getArgs().length==1){
			try{
				String arg=new BufferedReader(new InputStreamReader(System.in)).readLine();
				urls.add(arg);
			}catch(Exception e){
				error("Can't read transfer URL from stdin.",e);
				endProcessing(ERROR_CLIENT);
			}
		}
		else{
			for(int i=1; i<getCommandLine().getArgs().length;i++){
				urls.add(getCommandLine().getArgs()[i]);
			}
		}
		try{
			for(String url: urls) {
				doCheck(url);
			}
		}catch(Exception e){
			error("Can't check status.",e);
			endProcessing(ERROR);
		}
	}

	protected void doCheck(String url)throws Exception{
		TransferControllerClient tcc = new TransferControllerClient(new Endpoint(url),
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
		Long transferred = tcc.getTransferredBytes();
		Status status = tcc.getStatus();
		message(tcc.getEndpoint().getUrl()+" "+status+", <"+up.getHumanReadable(transferred)+"> bytes");
	}
	
	@Override
	public String getName() {
		return "copy-file-status";
	}

	@Override
	public String getSynopsis() {
		return "Checks the status of server-to-server transfers.";
	}
	
	@Override
	public String getDescription(){
		return "check status of server-to-server transfers";
	}
	
	@Override
	public String getArgumentList(){
		return "TransferURL(s)";
	}
	@Override
	public String getCommandGroup(){
		return "Data management";
	}
}
