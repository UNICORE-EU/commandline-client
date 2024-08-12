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
import eu.unicore.ucc.UCCException;
import eu.unicore.ucc.util.JSONUtil;

/**
 * Gets detailed information about a file.
 * 
 * @author K. Benedyczak
 */
public class GetFileProperties extends SMSOperation {
	
	protected DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	protected UnitParser unitParser = UnitParser.getCapacitiesParser(1);
	
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
		return "Retrieves file status: permissions, ACL, owner and more.";
	}
	
	@Override
	public String getDescription(){
		return "show detailed file information";
	}
	
	@Override
	public String getArgumentList(){
		return "<[Storage-URL#]/path or [u6://SITENAME/[JobId|StorageName]]/path>";
	}

	@Override
	public void process() throws Exception {
		super.process();
		boolean human = getBooleanOption(OPT_HUMAN_LONG, OPT_HUMAN);
		if (human)
			verbose("Human friendly number format.");
		boolean showMetadata = getBooleanOption(OPT_SHOW_META_LONG, OPT_SHOW_META);
		if (showMetadata)
			verbose("Showing metadata.");
		
		CommandLine cmdLine = getCommandLine(); 
		if (cmdLine.getArgs().length != 2) {
			throw new UCCException("Wrong number of arguments");
		}
		String target = cmdLine.getArgs()[1];
		verbose("Getting file properties of " + target);
		StorageClient sms = getStorageClient(target);
		FileListEntry gridFile = sms.stat(getPathAtStorage(target));
		String msg = formatStat(gridFile, human, showMetadata);
		message(msg);
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
			} catch (JSONException e)
			{
				error("Can't print metadata", e);
			}
		}
		return sb.toString();
	}

}





