package eu.unicore.ucc.actions.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import eu.unicore.client.Endpoint;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.client.data.TransferControllerClient.Status;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.actions.ActionBase;

/**
 * check the status of asynchronous copy-file operations)
 */
public class CopyFileStatus extends ActionBase {

	private final UnitParser up = UnitParser.getCapacitiesParser(3);

	@Override
	public void process() throws Exception {
		super.process();
		List<String> urls = new ArrayList<>();
		if(getCommandLine().getArgs().length==1){
			String arg=new BufferedReader(new InputStreamReader(System.in)).readLine();
			urls.add(arg);
		}
		else{
			for(int i=1; i<getCommandLine().getArgs().length;i++){
				urls.add(getCommandLine().getArgs()[i]);
			}
		}
		for(String url: urls) {
			doCheck(url);
		}
	}

	private void doCheck(String url)throws Exception{
		TransferControllerClient tcc = new TransferControllerClient(new Endpoint(url),
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
		Long transferred = tcc.getTransferredBytes();
		Status status = tcc.getStatus();
		console.info("{} {}, <{}> bytes", tcc.getEndpoint().getUrl(), status, up.getHumanReadable(transferred));
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
		return CMD_GRP_DATA;
	}
}
