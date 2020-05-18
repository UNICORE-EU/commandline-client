package eu.unicore.ucc.actions.data;

import java.io.IOException;

import org.apache.commons.cli.OptionBuilder;

import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.io.Location;
import jline.console.ConsoleReader;

/**
 * removes a remote file/directory resource
 * 
 * @see Location
 *
 * @author schuller
 */
public class RM extends SMSOperation {

	protected Location targetDesc;

	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt("quiet")
				.withDescription("quiet mode")
				.isRequired(false)
				.create("q")
				);
	}
	
	@Override
	public void process(){
		super.process();
		boolean quiet = getBooleanOption("quiet", "q");
		String target = getCommandLine().getArgs()[1];
		StorageClient sms = getStorageClient(target);
		try{
			String dir = getPathAtStorage(target); 
			if(!quiet){
				boolean confirmed = confirm();
				if(!confirmed){
					verbose("Cancelled.");
					return;
				}
			}
			sms.getFileClient(dir).delete();
		}catch(Exception ex){
			error("Can't contact storage service.",ex);
			endProcessing(ERROR);
		}
		
	}
	
	protected boolean confirm(){
		ConsoleReader r = null;
		try{
			r = new ConsoleReader();
			String line=r.readLine("This will delete a remote file/directory, are you sure? [Y]");
			return line.length()==0  || line.startsWith("y") || line.startsWith("Y");
		}catch(IOException igored){}
		finally{
			if(r!=null) r.shutdown();
		}
		
		return false;
	}
	@Override
	public String getName() {
		return "rm";
	}

	@Override
	public String getSynopsis() {
		return "remove a file or directory on a remote storage.";
	}
	
	@Override
	public String getDescription(){
		return "remove a remote file or directory";
	}
	
	@Override
	public String getArgumentList(){
		return "[unicore6://SITENAME/[JobId|StorageName]]";
	}

	@Override
	public String getCommandGroup(){
		return "Data management";
	}
}
