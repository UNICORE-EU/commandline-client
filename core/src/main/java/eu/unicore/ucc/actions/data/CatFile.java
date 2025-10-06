package eu.unicore.ucc.actions.data;

import java.io.OutputStream;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.StorageConstants;
import eu.unicore.ucc.io.FileDownloader;
import eu.unicore.ucc.io.FileTransferBase.Mode;
import eu.unicore.ucc.io.Location;

/**
 * cat a file from a remote SMS (or 'raw' http(s) URL)
 * 
 * @author schuller
 */
public class CatFile extends FileOperation implements StorageConstants {

	private Location sourceDesc;

	private StorageClient sms;

	@Override
	public void process() throws Exception {
		super.process();
		if(getCommandLine().getArgs().length>1){
			for(int i=1; i<getCommandLine().getArgs().length;i++){
				String source=getCommandLine().getArgs()[i];
				sourceDesc = createLocation(source);
				if(sourceDesc.isRaw()){
					console.debug("Source file URL {}", sourceDesc.getSmsEpr());
					runRawTransfer(sourceDesc.getSmsEpr(), getStdout(), null);
				}
				else{
					console.debug("Source file URL {}", sourceDesc.getUnicoreURI());
					runFileDownloader();
				}
				System.out.println();
			}
		}
		else{
			throw new IllegalArgumentException("Please specify a remote file!");
		}
	}

	private void runFileDownloader()throws Exception{
		Endpoint e = new Endpoint(sourceDesc.getSmsEpr());
		sms = new StorageClient(e,
				configurationProvider.getClientConfiguration(sourceDesc.getSmsEpr()),
				configurationProvider.getRESTAuthN());
		
		FileDownloader exp = new FileDownloader(sourceDesc.getName(),"", Mode.NORMAL);
		String selectedProtocol = getEffectiveProtocol(sourceDesc);
		exp.setPreferredProtocol(selectedProtocol);
		exp.setExtraParameterSource(properties);
		exp.setTiming(timing);
		exp.setForceFileOnly(true);
		exp.setShowProgress(false);
		exp.setTargetStream(getStdout());
		if(startByte!=null){
			exp.setStartByte(startByte);
			exp.setEndByte(endByte);
		}
		exp.setStorageClient(sms);
		exp.callAndCheck();
	}

	OutputStream getStdout(){
		return System.out;
	}

	@Override
	public String getName() {
		return "cat";
	}

	@Override
	public String getSynopsis() {
		return "Prints a file from remote location to stdout";
	}

	@Override
	public String getDescription(){
		return "cat remote files";
	}

	@Override
	public String getArgumentList(){
		return "<file_url(s)>";
	}
}
