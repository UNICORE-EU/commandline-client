package eu.unicore.ucc.actions.data;

import java.util.Calendar;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.core.FileList;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCCException;

/**
 * lists a storage
 *
 * @author schuller
 */
public class LS extends SMSOperation {

	protected boolean detailed;

	protected boolean recurse;

	protected boolean human;

	protected boolean showMetadata;

	protected UnitParser unitParser=UnitParser.getCapacitiesParser(1);

	protected StorageClient sms;

	public String getName(){
		return "ls";
	}

	@Override
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(Option.builder(OPT_DETAILED)
				.longOpt(OPT_DETAILED_LONG)
				.desc("detailed listing")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_RECURSIVE)
				.longOpt(OPT_RECURSIVE_LONG)
				.desc("recurse into subdirectories")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_HUMAN)
				.longOpt(OPT_HUMAN_LONG)
				.desc("human-friendly format")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_SHOW_META)
				.longOpt(OPT_SHOW_META_LONG)
				.desc("Show metadata")
				.required(false)
				.build());
	}

	@Override
	public void process() throws Exception {
		super.process();
		detailed=getBooleanOption(OPT_DETAILED_LONG, OPT_DETAILED);
		if(detailed)verbose("Detailed listing.");
		recurse=getBooleanOption(OPT_RECURSIVE_LONG, OPT_RECURSIVE);
		if(recurse)verbose("Listing subdirectories.");
		human=getBooleanOption(OPT_HUMAN_LONG, OPT_HUMAN);
		if(human)verbose("Human friendly number format.");
		showMetadata=getBooleanOption(OPT_SHOW_META_LONG, OPT_SHOW_META);
		if(showMetadata)verbose("Showing metadata.");
		doProcess();
	}


	protected void doProcess() throws Exception {
		if(getCommandLine().getArgs().length<2){
			throw new UCCException("Please provide a storage address!");
		}
		String address=getCommandLine().getArgs()[1];
		sms = getStorageClient(address);
		String path = getPathAtStorage(address);
		if(path==null){
			if(getCommandLine().getArgs().length>2){
				path=getCommandLine().getArgs()[2];
			}	
		}
		if(path==null)path="/";
		doListing(path);
	}

	protected void doListing(String path) throws Exception {
		FileListEntry listing = sms.stat(path);
		if(listing.isDirectory){
			listDirectory(path);
		}
		else{
			listSingleFile(listing);
		}
	}

	protected void listDirectory(String path) throws Exception {
		FileList listing = sms.ls(path);
		for(FileListEntry f: listing.list(0, 1000)){
			listSingleFile(f);
			if(f.isDirectory && recurse){
				doListing(f.path);
			}
		}
	}

	protected void listSingleFile(FileListEntry file) throws Exception {
		lastLS=file;
		message(detailed? detailedListing(file):normalListing(file));
		if(!file.isDirectory && showMetadata){
			printMetadata(file);
		}
	}

	protected void printMetadata(FileListEntry meta)throws Exception{
		JSONObject metadata = sms.getFileClient(meta.path).getProperties().optJSONObject("metadata");
		if(metadata!=null) {
			message(metadata.toString(2));
		}else {
			message("(no metadata)");	
		}
	}

	protected String normalListing(FileListEntry f){
		return f.path;
	}

	public String detailedListing(FileListEntry f){
		String d=f.isDirectory?"d":"-";
		String size=human?unitParser.getHumanReadable(f.size):	String.valueOf(f.size);
		return String.format("%1s%2s %3s %4s %5$-30s",
						d, f.permissions, size, f.lastAccessed, f.path);
	}

	@Override
	public String getDescription(){
		return "lists files on a storage";
	}

	@Override
	public String getSynopsis(){
		return "Lists files on a storage. " +
		"The storage can be given by EPR or using the unicore6:// URL notation.";
	}

	@Override
	public String getArgumentList(){
		return "<EPR or u6://...> [path]";
	}

	public String format(Calendar c){
		return UnitParser.getSimpleDateFormat().format(c.getTime());
	}

	//for unit testing
	static FileListEntry lastLS;

	public static FileListEntry getLastLS(){
		return lastLS;
	}
}
