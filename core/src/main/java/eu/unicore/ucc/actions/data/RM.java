package eu.unicore.ucc.actions.data;

import org.apache.commons.cli.Option;
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

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_QUIET)
				.longOpt(OPT_QUIET_LONG)
				.desc("Quiet mode, don't ask for confirmation")
				.required(false)
				.build());
	}
	
	@Override
	public void process() throws Exception {
		super.process();
		boolean quiet = getBooleanOption(OPT_QUIET_LONG, OPT_QUIET);
		String target = getCommandLine().getArgs()[1];
		StorageClient sms = getStorageClient(target);
		String dir = getPathAtStorage(target); 
		if(!quiet){
			boolean confirmed = confirm();
			if(!confirmed){
				console.verbose("Cancelled.");
				return;
			}
		}
		sms.getFileClient(dir).delete();
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
}
