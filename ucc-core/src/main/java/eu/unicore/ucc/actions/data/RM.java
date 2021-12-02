package eu.unicore.ucc.actions.data;

import org.apache.commons.cli.OptionBuilder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.io.Location;

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
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_QUIET_LONG)
				.withDescription("Quiet mode, don't ask for confirmation")
				.isRequired(false)
				.create(OPT_QUIET)
				);
	}
	
	@Override
	public void process(){
		super.process();
		boolean quiet = getBooleanOption(OPT_QUIET_LONG, OPT_QUIET);
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
		LineReader r = null;
		try{
			r = LineReaderBuilder.builder().build();
			String line = r.readLine("This will delete a remote file/directory, are you sure? [Y]");
			return line.length()==0  || line.startsWith("y") || line.startsWith("Y");
		}finally{
			try{
				if(r!=null) r.getTerminal().close();
			}catch(Exception e) {}
		}
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
