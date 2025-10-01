package eu.unicore.ucc.actions.data;

import org.apache.commons.cli.Option;

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
				.get());
	}

	@Override
	public void process() throws Exception {
		super.process();
		boolean quiet = getBooleanOption(OPT_QUIET_LONG, OPT_QUIET);
		if (getCommandLine().getArgs().length < 2) {
			throw new IllegalArgumentException("Wrong number of arguments");
		}
		for(int i=1; i<getCommandLine().getArgs().length;i++){
			String target = getCommandLine().getArgs()[i];
			StorageClient sms = getStorageClient(target);
			String dir = getPathAtStorage(target); 
			if(!quiet){
				boolean confirmed = confirm(target);
				if(!confirmed){
					console.verbose("Cancelled.");
					continue;
				}
			}
			sms.getFileClient(dir).delete();
		}
	}

	private boolean confirm(String t){
		String line = UCC.getLineReader().readLine("This will delete remote file <"+t+">, "
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
