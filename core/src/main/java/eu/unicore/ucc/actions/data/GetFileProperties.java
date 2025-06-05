package eu.unicore.ucc.actions.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.util.JSONUtil;

/**
 * Gets detailed information about a file.
 * 
 * @author K. Benedyczak
 */
public class GetFileProperties extends SMSOperation {
	
	private DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private final UnitParser unitParser = UnitParser.getCapacitiesParser(1);

	@Override
	protected void createOptions(){
		super.createOptions();
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
	public String getName() {
		return "stat";
	}

	@Override
	public String getSynopsis() {
		return "Retrieves information about a file: size, permissions, ACL, owner and more.";
	}
	
	@Override
	public String getDescription(){
		return "show detailed file information";
	}

	@Override
	public void process() throws Exception {
		super.process();
		boolean human = getBooleanOption(OPT_HUMAN_LONG, OPT_HUMAN);
		if (human)
			console.verbose("Human friendly number format.");
		boolean showMetadata = getBooleanOption(OPT_SHOW_META_LONG, OPT_SHOW_META);
		if (showMetadata)
			console.verbose("Showing metadata.");
		CommandLine cmdLine = getCommandLine(); 
		if (cmdLine.getArgs().length < 2) {
			throw new IllegalArgumentException("Wrong number of arguments");
		}
		for(int i=1; i<getCommandLine().getArgs().length;i++){
			String target = cmdLine.getArgs()[i];	
			console.verbose("Getting file properties of <{}>", target);
			StorageClient sms = getStorageClient(target);
			FileListEntry gridFile = sms.stat(getPathAtStorage(target));
			console.info("{}", formatStat(gridFile, human, showMetadata));
		}
	}

	protected DateFormat isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	private String formatStat(FileListEntry f, boolean human, boolean showMetadata) {
		String lastModification = f.lastAccessed;
		try {
			lastModification = dateFormatter.format(isoDateFormatter.parse(f.lastAccessed));
		}catch(Exception ex) {}
		String size = human ? unitParser.getHumanReadable(f.size) :
			String.valueOf(f.size);
		String type = f.isDirectory ? "directory" : "normal file";

		StringBuilder sb = new StringBuilder();
		sb.append("File: ").append(f.path).append("\n");
		sb.append("Size: ").append(size).append("\n");
		sb.append("Type: ").append(type).append("\n");
		if (f.permissions != null) {
			sb.append("Permissions: ").append(f.permissions).append("\n");
		}
		if (lastModification != null) {
			sb.append("Last modification: ").append(lastModification).append("\n");
		}
		if (showMetadata && f.metadata != null) {
			JSONObject o = JSONUtil.asJSON(f.metadata);
			try
			{
				sb.append("Metadata: ").append(o.toString(2));
			} catch (JSONException e){}
		}
		return sb.toString();
	}

}





