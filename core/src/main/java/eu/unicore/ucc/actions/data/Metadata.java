package eu.unicore.ucc.actions.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.utils.TaskClient;
import eu.unicore.ucc.UCCException;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.io.Location;
import eu.unicore.ucc.util.JSONUtil;

/**
 * Metadata operations on a storage
 * 
 * @author schuller
 */
public class Metadata extends ActionBase {

	public static String OPT_COMMAND_LONG = "command";
	public static String OPT_COMMAND = "C";
	public static String OPT_STORAGE_LONG = "storage";
	public static String OPT_STORAGE = "s";
	public static String OPT_META_LONG = "metadata-service";
	public static String OPT_META = "m";
	public static String OPT_FILE_LONG = "file";
	public static String OPT_FILE = "f";
	public static String OPT_QUERY_LONG = "query";
	public static String OPT_QUERY = "q";
	public static String OPT_QUERYADV_LONG = "advanced-query";
	public static String OPT_QUERYADV = "a";
	public static String OPT_WAIT_LONG = "wait";
	public static String OPT_WAIT = "w";
	protected String storageURL;
	protected StorageClient sms;
	protected String command;
	protected String path;
	protected File file;
	protected String query;
	boolean advanced;
	boolean wait;

	public String getName() {
		return "metadata";
	}

	@Override
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(Option.builder(OPT_COMMAND)
				.longOpt(OPT_COMMAND_LONG)
				.desc("Metadata command: write, read, update, delete, start-extract, search")
				.required(true)
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_STORAGE)
				.longOpt(OPT_STORAGE_LONG)
				.desc("Storage address")
				.required(false)
				.hasArg()
				.argName("Storage")
				.build());
		getOptions().addOption(Option.builder(OPT_FILE)
				.longOpt(OPT_FILE_LONG)
				.desc("File containing metadata")
				.required(false)
				.hasArg()
				.argName("Filename")
				.build());
		getOptions().addOption(Option.builder(OPT_QUERY)
				.longOpt(OPT_QUERY_LONG)
				.desc("Query string for search")
				.required(false)
				.hasArg()
				.argName("Query")
				.build());
		getOptions().addOption(Option.builder(OPT_WAIT)
				.longOpt(OPT_WAIT_LONG)
				.desc("Wait for long-running tasks to finish")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_QUERYADV)
				.longOpt(OPT_QUERYADV_LONG)
				.desc("Advanced query")
				.required(false)
				.build());
	}

	@Override
	public void process() throws Exception {
		super.process();
		command = getOption(OPT_COMMAND_LONG, OPT_COMMAND);
		verbose("Operation = " + command);
		advanced = getBooleanOption(OPT_QUERYADV_LONG, OPT_QUERYADV);
		wait = getBooleanOption(OPT_WAIT_LONG, OPT_WAIT);
		query = getOption(OPT_QUERY_LONG, OPT_QUERY);
		String storage = getOption(OPT_STORAGE_LONG, OPT_STORAGE);
		if(storage==null){
			String desc=getArgument(1);
			// interpret path as a full UNICORE storage path
			Location loc = createLocation(desc);
			storage=loc.getSmsEpr();
			path=loc.getName();
		}
		sms = createStorageClient(storage);
		if (sms == null) {
			throw new UCCException("Cannot find the requested storage service!");
		}
		verbose("Accessing metadata for storage " + sms.getEndpoint().getUrl());
		String fName = getOption(OPT_FILE_LONG, OPT_FILE);
		if (fName != null) {
			file = new File(fName);
		}
		if ("search".startsWith(command)) {
			if (query == null) {
				throw new UCCException("Please provide a query string!");
			}
			doSearch();
		} else {
			if(path==null) {
				path = getArgument(1);
			}
			if ("read".startsWith(command)) {
				doGet();
			} else if ("write".startsWith(command)) {
				doWrite();
			} else if ("update".startsWith(command)) {
				doUpdate();
			} else if ("delete".startsWith(command)) {
				doDelete();
			} else if ("start-extract".startsWith(command)) {
				// TODO depth
				doStartExtract();
			} else {
				throw new UCCException("Unknown command : " + command, null);
			}
		}
	}

	protected void doGet() throws Exception {
		Map<String, String> result = sms.stat(path).metadata;
		String json = JSONUtil.asJSON(result).toString(2);
		message(json);
		if (file != null) {
			FileUtils.writeStringToFile(file, json, "UTF-8");
		}
		lastMeta.clear();
		lastMeta.putAll(result);
	}

	protected void doWrite() throws Exception {
		doSet(readData());
	}

	protected void doUpdate() throws Exception {
		Map<String, String> data = sms.stat(path).metadata;
		data.putAll(readData());
		doSet(data);
	}

	protected void doSet(Map<String, String> data) throws Exception {
		JSONObject update = new JSONObject();
		update.put("metadata", data);
		JSONObject reply = sms.getFileClient(path).setProperties(update);
		message(reply.toString(2));
		lastMeta.clear();
		lastMeta.putAll(data);
	}

	protected void doSearch() throws Exception {
		lastSearchResults.clear();
		List<String> files = sms.searchMetadata(query);
		verbose("Have <"+files.size()+"> results.");
		for(String f: files) {
			message("  "+f);
			lastSearchResults.add(f);
		}
	}

	protected void doDelete() throws Exception {
		doSet(new HashMap<>());
	}

	private String normalize(String path) {
		while(path.startsWith("//"))path=path.substring(1);
		if(!path.startsWith("/"))path = "/"+path;
		path = FilenameUtils.normalize(path, true);
		return path;
	}

	protected void doStartExtract() throws Exception {
		path = normalize(path);
		TaskClient tc = sms.getFileClient(path).startMetadataExtraction(10, new String[0]);
		if(tc!=null) {
			message("Extraction started, task = "+tc.getEndpoint().getUrl());
			properties.put(PROP_LAST_RESOURCE_URL, tc.getEndpoint().getUrl());
		}
		if(tc!=null && wait) {
			verbose("Waiting for extraction task <"+tc.getEndpoint().getUrl()+"> to finish...");
			while(!tc.isFinished())Thread.sleep(2000);
			JSONObject taskProps = tc.getProperties();
			JSONObject result = taskProps.optJSONObject("result", new JSONObject());
			message("Status: "+tc.getStatus());
			message("Result: \n"+result.toString(2));
		}
	}

	/**
	 * read metadata from file or stdin
	 * @throws Exception
	 */
	protected Map<String, String> readData() throws Exception {
		String json = null;
		if (file != null) {
			verbose("Reading data from the file: " + file.getName());
			json = FileUtils.readFileToString(file, "UTF-8");
		} else {
			//read from stdin
			verbose("Reading from stdIn (it does not work from within the shell)");

			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String str = "";
			StringBuilder buffer = new StringBuilder();
			do {
				System.out.print(">> ");
				str = in.readLine();
				if (str != null) {
					buffer.append(str);
				}
			} while (str != null && !str.isEmpty());

			json = buffer.toString();
		}
		JSONObject j = new JSONObject(json);
		return JSONUtil.asMap(j);
	}

	protected StorageClient createStorageClient(String storage) throws Exception {
		Location td = createLocation(storage);
		Endpoint epr = new Endpoint(td.getSmsEpr());
		StorageClient sms = new StorageClient(epr,
				configurationProvider.getClientConfiguration(epr.getUrl()),
				configurationProvider.getRESTAuthN());
		verbose("Storage " + td.getSmsEpr());
		if (!sms.supportsMetadata()) {
			throw new UCCException ("Storage does not support metadata.");
		}
		return sms;
	}

	@Override
	public String getDescription() {
		return "perform operations on metadata";
	}

	@Override
	public String getSynopsis() {
		return "Performs operations on metadata. "
				+ "Either a storage or directly a metadata service can be given.";
	}

	@Override
	public String getArgumentList() {
		return "[resource-name]";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_DATA;
	}
	
	public static final Collection<String>getCommands(){
		return Arrays.asList("write", "read", "update", "delete", "start-extract", "search");
	}

	private String getArgument(final int argN) {
		try {
			return getCommandLine().getArgs()[argN];
		} catch (IndexOutOfBoundsException ex) {
			throw new IllegalArgumentException("This method requires at least " + argN + " arguments", null);
		}
	}

	public final static Map<String, String> lastMeta = new HashMap<>();
	public final static Collection<String> lastSearchResults = new HashSet<>();
}
