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
	public static String OPT_WAIT_LONG = "wait";
	public static String OPT_WAIT = "w";

	private StorageClient sms;
	private String command;
	private String path;
	private File file;
	private String query;
	private boolean wait;

	public String getName() {
		return "metadata";
	}

	@Override
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(Option.builder(OPT_COMMAND)
				.longOpt(OPT_COMMAND_LONG)
				.desc("Metadata command ("+commands+")")
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
	}

	@Override
	public void process() throws Exception {
		super.process();
		command = getOption(OPT_COMMAND_LONG, OPT_COMMAND);
		console.verbose("Operation = {}", command);
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
			throw new IllegalArgumentException("Cannot find the requested storage service!");
		}
		console.verbose("Accessing metadata for storage {}", sms.getEndpoint().getUrl());
		String fName = getOption(OPT_FILE_LONG, OPT_FILE);
		if (fName != null) {
			file = new File(fName);
		}
		if ("search".startsWith(command)) {
			if (query == null) {
				throw new IllegalArgumentException("'search' requires a query string!");
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
				throw new IllegalArgumentException("Unknown command : " + command, null);
			}
		}
	}

	private void doGet() throws Exception {
		Map<String, String> result = sms.stat(path).metadata;
		String json = JSONUtil.asJSON(result).toString(2);
		console.info("{}", json);
		if (file != null) {
			FileUtils.writeStringToFile(file, json, "UTF-8");
		}
		lastMeta.clear();
		lastMeta.putAll(result);
	}

	private void doWrite() throws Exception {
		doSet(readData());
	}

	private void doUpdate() throws Exception {
		Map<String, String> data = sms.stat(path).metadata;
		data.putAll(readData());
		doSet(data);
	}

	private void doSet(Map<String, String> data) throws Exception {
		JSONObject update = new JSONObject();
		update.put("metadata", data);
		JSONObject reply = sms.getFileClient(path).setProperties(update);
		console.info("{}", reply.toString(2));
		lastMeta.clear();
		lastMeta.putAll(data);
	}

	private void doSearch() throws Exception {
		lastSearchResults.clear();
		List<String> files = sms.searchMetadata(query);
		console.verbose("Have <{}> results.", files.size());
		for(String f: files) {
			console.info("  {}", f);
			lastSearchResults.add(f);
		}
	}

	private void doDelete() throws Exception {
		doSet(new HashMap<>());
	}

	private String normalize(String path) {
		while(path.startsWith("//"))path=path.substring(1);
		if(!path.startsWith("/"))path = "/"+path;
		path = FilenameUtils.normalize(path, true);
		return path;
	}

	private void doStartExtract() throws Exception {
		path = normalize(path);
		TaskClient tc = sms.getFileClient(path).startMetadataExtraction(10, new String[0]);
		if(tc!=null) {
			console.info("Extraction started, task = {}", tc.getEndpoint().getUrl());
			properties.put(PROP_LAST_RESOURCE_URL, tc.getEndpoint().getUrl());
		}
		if(tc!=null && wait) {
			console.verbose("Waiting for extraction task <{}> to finish...", tc.getEndpoint().getUrl());
			while(!tc.isFinished())Thread.sleep(2000);
			JSONObject taskProps = tc.getProperties();
			JSONObject result = taskProps.optJSONObject("result", new JSONObject());
			console.info("Status: {}", tc.getStatus());
			console.info("Result: \n{}", result.toString(2));
		}
	}

	/**
	 * read metadata from file or stdin
	 * @throws Exception
	 */
	private Map<String, String> readData() throws Exception {
		String json = null;
		if (file != null) {
			console.verbose("Reading data from file: <{}>", file.getName());
			json = FileUtils.readFileToString(file, "UTF-8");
		} else {
			//read from stdin
			console.verbose("Reading from stdin (it does not work from within the shell)");
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

	private StorageClient createStorageClient(String storage) throws Exception {
		Location td = createLocation(storage);
		Endpoint epr = new Endpoint(td.getSmsEpr());
		StorageClient sms = new StorageClient(epr,
				configurationProvider.getClientConfiguration(epr.getUrl()),
				configurationProvider.getRESTAuthN());
		console.verbose("Storage {}", td.getSmsEpr());
		if (!sms.supportsMetadata()) {
			throw new Exception("Storage does not support metadata.");
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
	
	@Override
	public Collection<String> getAllowedOptionValues(String option) {
		if(OPT_COMMAND.equals(option)) {
			return commands;
		}
		return null;
	}

	private static final Collection<String> commands = Arrays.asList(
		"write", "read", "update", "delete", "start-extract", "search");

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
