package eu.unicore.ucc.actions.data;

import org.apache.commons.cli.Option;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.io.Location;

/**
 * removes a remote file/directory resource
 * 
 * @see Location
 *
 * @author schuller
 */
public class RM extends SMSOperation {

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

	private boolean confirm(){
		String line = UCC.getLineReader().readLine("This will delete a remote file/directory, "
				+ "are you sure? [Y]");
		return line.length()==0  || line.startsWith("y") || line.startsWith("Y");
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

}
