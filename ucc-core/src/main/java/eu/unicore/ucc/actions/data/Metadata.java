package eu.unicore.ucc.actions.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import de.fzj.unicore.ucc.helpers.EndProcessingException;
import de.fzj.unicore.ucc.util.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.io.Location;

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
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(OptionBuilder.withLongOpt(OPT_COMMAND_LONG).
				withDescription("Metadata command: write, read, update, delete, start-extract, search").
				isRequired(true).
				hasArg().
				create(OPT_COMMAND));
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_STORAGE_LONG).
				withDescription("Storage address").
				isRequired(false).
				withArgName("Storage").
				hasArg().
				create(OPT_STORAGE));
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_FILE_LONG).
				withDescription("File containing metadata").
				isRequired(false).
				withArgName("Filename").
				hasArg().
				create(OPT_FILE));
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_QUERY_LONG).
				withDescription("Query string for search").
				isRequired(false).
				withArgName("Query").
				hasArg().
				create(OPT_QUERY));
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_WAIT_LONG).
				withDescription("Wait for long-running tasks to finish").
				isRequired(false).
				create(OPT_WAIT));
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_QUERYADV_LONG)
				.withDescription("Advanced query")
				.isRequired(false)
				.create(OPT_QUERYADV));
	}

	@Override
	public void process() {
		super.process();
		command = getOption(OPT_COMMAND_LONG, OPT_COMMAND);
		verbose("Operation = " + command);
		advanced = getBooleanOption(OPT_QUERYADV_LONG, OPT_QUERYADV);
		wait = getBooleanOption(OPT_WAIT_LONG, OPT_WAIT);
		query = getOption(OPT_QUERY_LONG, OPT_QUERY);
		String storage = getOption(OPT_STORAGE_LONG, OPT_STORAGE);
		if(storage==null){
			String desc=getArgument(1);
			//try to interpret path as a full UNICORE storage path
			Location loc = createLocation(desc);
			storage=loc.getSmsEpr();
			path=loc.getName();
		}
		sms = createStorageClient(storage);

		if (sms == null) {
			message("Cannot find the requested storage service!");
			printUsage();
			endProcessing(ERROR_CLIENT);
		}

		verbose("Accessing metadata for storage " + sms.getEndpoint().getUrl());

		String fName = getOption(OPT_FILE_LONG, OPT_FILE);
		if (fName != null) {
			file = new File(fName);
		}

		if ("read".startsWith(command)) {
			if(path==null)path = getArgument(1);
			doGet();
		} else if ("write".startsWith(command)) {
			if(path==null)path = getArgument(1);
			doWrite();
		} else if ("update".startsWith(command)) {
			if(path==null)path = getArgument(1);
			doUpdate();
		} else if ("delete".startsWith(command)) {
			if(path==null)path = getArgument(1);
			doDelete();
		} else if ("start-extract".startsWith(command)) {
			path = getArgument(1);
			//TODO depth

			doStartExtract();
		} else if ("search".startsWith(command)) {
			if (query == null) {
				error("Please provide a query string!", null);
				endProcessing(1);
			}
			doSearch();
		} else {
			error("Unknown command : " + command, null);
			endProcessing(1);
		}

	}

	protected void doGet() {
		try {
			Map<String, String> result = sms.stat(path).metadata;
			String json = JSONUtil.asJSON(result).toString(2);
			message(json);
			if (file != null) {
				FileUtils.writeStringToFile(file, json, "UTF-8");
			}
			lastMeta = result;
		} catch (Exception ex) {
			error("Error getting metadata for <" + path + ">", ex);
			endProcessing(1);
		}
	}

	protected void doWrite() {
		try {
			doSet(readData());
		} catch (Exception ex) {
			error("Error writing metadata for <" + path + ">", ex);
			endProcessing(1);
		}
	}
	
	protected void doUpdate() {
		try {
			Map<String, String> data = sms.stat(path).metadata;
			data.putAll(readData());
			doSet(data);
		} catch (Exception ex) {
			error("Error creating/updating metadata for <" + path + ">", ex);
			endProcessing(1);
		}
	}
	
	protected void doSet(Map<String, String> data) throws Exception {
		JSONObject update = new JSONObject();
		update.put("metadata", data);
		JSONObject reply = sms.getFileClient(path).setProperties(update);
		message(reply.toString(2));
		lastMeta = data;
	}

	protected void doSearch() {
		// TODO
		error("Not yet implemented",null);
	}

	protected void doDelete() {
		try {
			doSet(new HashMap<>());
		} catch (Exception ex) {
			error("Error deleting metadata for <" + path + ">", ex);
			endProcessing(1);
		}
	}

	protected void doStartExtract() {
		try {
			// TODO
			error("Not yet implemented",null);
		} catch (Exception ex) {
			error("Error starting metadata extraction for <" + path + ">", ex);
			endProcessing(1);
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

	protected StorageClient createStorageClient(String storage) {
		Location td = null;
		StorageClient sms = null;
		try {
			td = createLocation(storage);
			Endpoint epr = new Endpoint(td.getSmsEpr());
			sms = new StorageClient(epr,
					configurationProvider.getClientConfiguration(epr.getUrl()),
					configurationProvider.getRESTAuthN());
			verbose("Storage " + td.getSmsEpr());
			return sms;
		} catch (Exception e) {
			error("Can't access :" + storage, e);
			endProcessing(ERROR);
		}

		try {
			if (!sms.supportsMetadata()) {
				message("Storage does not support metadata.");
				endProcessing(ERROR);
			}
		} catch (EndProcessingException epe) {
			throw epe;
		} catch (Exception e) {
			error("Can't get metadata service for :" + storage, e);
			endProcessing(ERROR);
		}
		return null;
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
	public String getCommandGroup() {
		return "Data management";
	}

	private String getArgument(final int argN) {
		try {
			return getCommandLine().getArgs()[argN];
		} catch (IndexOutOfBoundsException ex) {
			error("This method requires at least " + argN + " arguments", null);
			endProcessing();
		}
		return null;

	}

	/**
	 * for unit tests
	 * @return the result of the last operation
	 */
	public static Map<String, String> getLastMeta() {
		return lastMeta;
	}
	private static Map<String, String> lastMeta;
}
