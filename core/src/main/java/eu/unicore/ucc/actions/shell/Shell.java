package eu.unicore.ucc.actions.shell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;

import eu.unicore.client.Endpoint;
import eu.unicore.ucc.Command;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.UCCOptions;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.authn.UsernameAuthN;
import eu.unicore.ucc.util.PropertyVariablesResolver;
import eu.unicore.ucc.util.Spawner;

/**
 * a UCC command that starts an interactive session
 * 
 * @author schuller
 */
public class Shell extends ActionBase {

	private static final String HISTORY_FILEKEY = "ucc-shell-history";
	private static final String OPT_FILE="f";
	private static final String OPT_FILE_LONG="file";

	private File commandFile;

	private History history;

	@Override
	public String getDescription() {
		return "start an interactive UCC session";
	}

	@Override
	public String getName() {
		return "shell";
	}

	@Override
	public String getSynopsis() {
		return "Starts an interactive UCC session that allows you to "
				+ "run multiple UCC commands, and offers convenient features "
				+ "such as URL completion, shell history and more.";
	}

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_FILE)
				.longOpt(OPT_FILE_LONG)
				.desc("Read input from this file instead stdin")
				.required(false)
				.argName("CommandsFile")
				.hasArg()
				.get());
	}

	@Override
	public void process() throws Exception {
		super.process();
		String fileName=getOption(OPT_FILE_LONG, OPT_FILE);
		if(fileName!=null){
			commandFile=new File(fileName);
		}
		run();
	}

	@Override
	protected void testRegistryConnection() throws Exception {
		// setup linereader here, if we must query for passwords
		setupLineReader();
		super.testRegistryConnection();
		for(Endpoint ep: registry.listEntries()) {
			URLCompleter.registerSiteURL(ep.getUrl());
		}
	}

	private List<String> internalCommands = Arrays.asList( "set", "unset", "system", "!",
			"help", "help-auth", "version",
			"exit", "quit" );

	private void setupLineReader() {
		UCCCompleter c = null;
		if(commandFile==null){
			Set<String> cmds = new HashSet<>();
			cmds.addAll(UCC.cmds.keySet());
			for(String s: internalCommands) {
				cmds.add(s);
			}
			c = new UCCCompleter(cmds, this.configurationProvider);
		}
		LineReader is = LineReaderBuilder.builder().completer(c).build();
		if(commandFile==null) {
			history = getHistory(is);
		}
		UCC.setLineReader(is);
	}

	private void run(){
		Command.quitAfterPrintingUsage=false;
		LineReader is = UCC.getLineReader();
		try{
			if(commandFile!=null){
				is.addCommandsInBuffer(FileUtils.readLines(commandFile, "UTF-8"));
				is.addCommandsInBuffer(Arrays.asList("exit"));
			}
			else {
				history = getHistory(is);
			}
			System.out.println(getHeader());
			System.out.println("UCC "+UCC.getVersion());
			System.out.println("Welcome to the UCC shell. Enter 'help' for a list of commands. Enter 'exit' to quit.");
			while(true){
				UCC.console.setPrefix("[ucc "+getName()+"]");
				String s = null;
				try {
					s = commandFile!=null ? is.readLine() : is.readLine("ucc>");
				}catch(Exception uie) {}
				if(s==null){
					s="exit";
				}
				s = s.trim();
				if(s.isEmpty() || s.startsWith("#"))continue;

				if("exit".equalsIgnoreCase(s) || "quit".equalsIgnoreCase(s)){
					console.info("\nGoodbye.");
					return;
				}
				if(s.startsWith("help-auth")){
					String[] sub = s.split(" +");
					String method = null;
					if(sub.length>1)method = sub[1];
					UCC.printAuthNUsage(method);
					continue;
				}
				if(s.startsWith("help")){
					String[] sub = s.split(" +");
					String method = null;
					if(sub.length>1)method = sub[1];
					if(method!=null) {
						var cmd = UCC.getCommand(method);
						if(cmd!=null) {
							cmd.printUsage();
							continue;
						}
						else if(internalCommands.contains(method)){
							printShellHelp();
							continue;
						}else {
							numberOfErrors++;
							System.err.println("No such command '"+method+"'");
						}
					}
					UCC.printUsage(false);
					printShellHelp();
					continue;
				}
				// it is a command
				String[] args = parseCmdline(s);
				if(UCC.console.isVerbose()) {
					StringBuilder sb = new StringBuilder();
					for(String a: args)sb.append(a).append(" ");
					console.verbose("{}", sb);
				}
				try{
					if(processSpecial(args)){
						continue;
					}
					String command = args[0];
					if(UCC.cmds.get(command)==null){
						console.info("No such command: {}", args[0]);
						continue;
					}
					if(UCC.console.isVerbose())properties.put(OPT_VERBOSE_LONG,"true");
					String authNMethod = getOption(OPT_AUTHN_METHOD_LONG, OPT_AUTHN_METHOD, UsernameAuthN.NAME);
					properties.put(OPT_AUTHN_METHOD_LONG, authNMethod);
					if(acceptAllIssuers) {
						properties.put(OPT_AUTHN_ACCEPT_ALL_LONG, "true");
					}
					new Spawner(this, args).run();
				}
				catch(ParseException pex){
					numberOfErrors++;
					console.error(pex, "Error parsing commandline arguments.");
				}
				catch(Exception ex){
					numberOfErrors++;
					console.error(ex, "Error processing command");
				}
			}
		} catch (Exception e) {
			console.error(e, "Error");
		} finally {
			if (history != null){
				try{
					history.save();
				}catch(Exception ex){}
			}
			try{
				if (is != null) is.getTerminal().close();
			}catch(Exception ex){}
		}
	}

	/**
	 * handle special "shell" commands like 'set'
	 * @param args
	 */
	private boolean processSpecial(String[] args) throws Exception {
		args = Spawner.expandArgs(args, properties);
		String cmd = args[0];
		if("set".equalsIgnoreCase(cmd)){
			handleSet(args);
		}
		else if("unset".equalsIgnoreCase(cmd)){
			handleUnset(args);
		}
		else if("system".equalsIgnoreCase(cmd) || "!".equals(cmd) ) {
			handleSystem(args);
		}
		else if("version".equalsIgnoreCase(cmd)) {
			handleVersion();
		}
		else {
			return false;
		}
		return true;
	}

	private void handleSet(String[] args) throws IOException {
		if(args.length==1){
			// "set" alone: print properties
			for(Object keyObj: properties.keySet()){
				String key=String.valueOf(keyObj);
				String val=properties.getProperty(key);
				//naive, but should work rather well :-)
				if(key.toLowerCase().contains("password")){
					console.info("{}=*", key);
				}
				else{
					console.info("{}={}", key, val);
				}
			}
		}
		else {
			// set property
			String[] paramArgs = new String[args.length-1];
			System.arraycopy(args, 1, paramArgs, 0, paramArgs.length);
			properties.putAll(PropertyVariablesResolver.getParameters(paramArgs));
			boolean verbose = UCC.console.isVerbose();
			verbose = UCCOptions.isTrue(properties.getProperty("verbose", String.valueOf(verbose)));
			UCC.console.setVerbose(verbose);
			boolean debug = UCC.console.isDebug();
			debug = UCCOptions.isTrue(properties.getProperty("UCC_DEBUG", String.valueOf(debug)));
			UCC.console.setDebug(debug);
		}
	}

	private void handleSystem(String[] args) throws Exception {
		if(args.length<2)return;
		String[] paramArgs = new String[args.length-1];
		System.arraycopy(args, 1, paramArgs, 0, paramArgs.length);
		ProcessBuilder pb = new ProcessBuilder(Arrays.asList(paramArgs));
		pb.inheritIO();
		Process p = pb.start();
		p.waitFor();
	}

	private void handleUnset(String[] args){
		for(int i=1; i<args.length;i++){
			properties.remove(args[i]);
		}
	}

	private void handleVersion(){
		UCC.printVersion();
	}

	private void printShellHelp(){
		System.err.println("Additional commands in the UCC shell:");
		System.err.println(" set [name=value]... - show variables / set a variable");
		System.err.println(" unset <name>...     - unset a variable");
		System.err.println(" system ...          - run a system command (also: '! ...'");
		System.err.println(" version             - show version info");
	}

	@Override
	protected boolean requireRegistry() {
		return false;
	}

	private History getHistory(LineReader lineReader) {
		try{
			String historyFile = properties.getProperty(HISTORY_FILEKEY);
			if(historyFile == null){
				historyFile=System.getProperty("user.home")+File.separator+".ucc"
						+File.separator+HISTORY_FILEKEY;
			}
			lineReader.setVariable(LineReader.HISTORY_FILE, historyFile);
			return new DefaultHistory(lineReader);
		}
		catch(Exception e){
			console.verbose("Cannot setup history");
			return null;
		}
	}

	private static Pattern p = Pattern.compile("\"([^\"]*)\"|(\\S+)");

	private int numberOfErrors = 0;

	public int getNumberOfErrors(){
		return numberOfErrors;
	}

	static String[] parseCmdline(String cmdArgs) throws IOException {
		List<String>result = new ArrayList<>();
		Matcher m = p.matcher(cmdArgs);
		while (m.find()) {
			String val;
			if (m.group(1) != null) {
				val = m.group(1);
			} else {
				val = m.group(2);
			}
			result.add(val);
		}
		return result.toArray(new String[result.size()]);
	}

	private String getHeader() {
		String s = _newline
				+ " _    _ _   _ _____ _____ ____  _____  ______" + _newline
				+ "| |  | | \\ | |_   _/ ____/ __ \\|  __ \\|  ____|" + _newline
				+ "| |  | |  \\| | | || |   | |  | | |__) | |__"+ _newline
				+ "| |  | | . ` | | || |   | |  | |  _  /|  __|"+ _newline
				+ "| |__| | |\\  |_| |_ |____ |__| | | \\ \\| |____"+ _newline
				+ " \\____/|_| \\_|_____\\_____\\____/|_|  \\_\\______|";
		return s;
	}
}
