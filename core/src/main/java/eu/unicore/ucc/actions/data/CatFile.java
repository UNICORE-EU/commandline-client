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

	protected Location sourceDesc;

	protected StorageClient sms;

	@Override
	public void process() {
		super.process();

		if(getCommandLine().getArgs().length>1){
			for(int i=1; i<getCommandLine().getArgs().length;i++){
				String source=getCommandLine().getArgs()[1];
				sourceDesc = createLocation(source);
				try{
					if(sourceDesc.isRaw()){
						verbose("Source file URL "+sourceDesc.getSmsEpr());
						runRawTransfer(sourceDesc.getSmsEpr(), getStdout(), null);
					}
					else{
						verbose("Source file URL "+sourceDesc.getUnicoreURI());
						runFileDownloader();
					}
				}catch(Exception e){
					error("Can't get file.",e);
					endProcessing(ERROR);
				}
				System.out.println();
			}
		}
		else{
			error("Please specify a remote file!",null);
			endProcessing(ERROR);
		}
	}

	private void runFileDownloader()throws Exception{
		Endpoint e = new Endpoint(sourceDesc.getSmsEpr());
		sms = new StorageClient(e,
				configurationProvider.getClientConfiguration(sourceDesc.getSmsEpr()),
				configurationProvider.getRESTAuthN());
		
		FileDownloader exp=new FileDownloader(sourceDesc.getName(),"", Mode.NORMAL);
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
		exp.perform(sms, this);
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
		return "<file_url>";
	}
	@Override
	public String getCommandGroup(){
		return "Data management";
	}

}
