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

import eu.unicore.ucc.Command;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.authn.KeystoreAuthN;
import eu.unicore.ucc.helpers.EndProcessingException;
import eu.unicore.ucc.util.PropertyVariablesResolver;

/**
 * a UCC command that starts an interactive session
 * 
 * @author schuller
 */
public class Shell extends ActionBase {

	public static final String HISTORY_FILEKEY = "ucc-shell-history";

	public static final String OPT_FILE="f";
	public static final String OPT_FILE_LONG="file";

	private File commandFile=null;

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
		return "Starts an interactive UCC session that allows you to " +
				"multiple run UCC commands.";
	}

	/**
	 *  creates options for the shell command
	 */
	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_FILE)
				.longOpt(OPT_FILE_LONG)
				.desc("Read input from this file instead stdin")
				.required(false)
				.argName("CommandsFile")
				.hasArg()
				.build());
	}

	@Override
	public void process(){
		super.process();
		String fileName=getOption(OPT_FILE_LONG, OPT_FILE);
		if(fileName!=null){
			commandFile=new File(fileName);
		}
		run();
	}

	private String[]internalCommands = { "set", "unset", "system", "!",
			"help", "help-auth", "version",
			"exit", "quit" };

	public void run(){
		Command.quitAfterPrintingUsage=false;
		LineReader is = null;
		History history = null;
		try{
			if(commandFile!=null){
				is = LineReaderBuilder.builder().build();
				is.addCommandsInBuffer(FileUtils.readLines(commandFile, "UTF-8"));
				is.addCommandsInBuffer(Arrays.asList("exit"));
			}
			else{
				Set<String> cmds = new HashSet<>();
				cmds.addAll(UCC.cmds.keySet());
				for(String s: internalCommands) {
					cmds.add(s);
				}
				is = LineReaderBuilder.builder()
						.completer(new UCCCompleter(cmds, this.configurationProvider))
						.build();
				history = getHistory(is);
			}
			System.out.println(getHeader());
			System.out.println("UCC "+UCC.getVersion());
			System.out.println("Welcome to the UCC shell. Enter 'help' for a list of commands. Enter 'exit' to quit.");
			while(true){
				UCC.getConsoleLogger().setPrefix("[ucc "+getName()+"]");
				String s = null;
				try {
					s=commandFile!=null?is.readLine():is.readLine("ucc>");
				}catch(Exception uie) {}
				if(s==null){
					s="exit";
				}
				s=s.trim();
				if(s.isEmpty())continue;

				if("exit".equalsIgnoreCase(s) || "quit".equalsIgnoreCase(s)){
					message("");
					message("Goodbye.");
					return;
				}
				if("help".equalsIgnoreCase(s)){
					UCC.printUsage(false);
					printShellHelp();
					continue;
				}
				if(s.startsWith("help-auth")){
					String[] sub = s.split(" +");
					String method = null;
					if(sub.length>1)method = sub[1];
					UCC.printAuthNUsage(method);
					continue;
				}

				//else it is a command
				String[] args = parseCmdlineWithVars(s);
				if(UCC.getConsoleLogger().isVerbose()) {
					StringBuilder sb = new StringBuilder();
					for(String a: args)sb.append(a).append(" ");
					verbose(sb.toString());
				}
				try{
					//check if it is a special command
					if(processSpecial(args)){
						continue;
					}
					String command = args[0];
					if(UCC.cmds.get(command)==null){
						message("No such command: "+args[0]);
						continue;
					}

					Command cmd = UCC.initCommand(args,false);
					//inherit properties
					if(UCC.getConsoleLogger().isVerbose())properties.put(OPT_VERBOSE_LONG,"true");
					String authNMethod = getOption(OPT_AUTHN_METHOD_LONG, OPT_AUTHN_METHOD, 
							KeystoreAuthN.X509);
					properties.put(OPT_AUTHN_METHOD_LONG, authNMethod);
					if(acceptAllIssuers) {
						properties.put(OPT_AUTHN_ACCEPT_ALL_LONG, "true");
					}
					cmd.setProperties(properties);
					cmd.setPropertiesFile(propertiesFile);
					UCC.getConsoleLogger().setPrefix("[ucc "+cmd.getName()+"]");
					cmd.process();
					cmd.postProcess();
				}catch(EndProcessingException epe){
					//OK just read next command
				}
				catch(ParseException pex){
					error("Error parsing commandline arguments.",pex);
				}
				catch(Exception ex){
					error("Error processing command", ex);
				}
			}
		} catch (Exception e) {
			error("Error",e);
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
	protected boolean processSpecial(String[] args) throws Exception {
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

	protected void handleSet(String[] args) throws IOException {
		if(args.length==1){
			//print properties
			for(Object keyObj: properties.keySet()){
				String key=String.valueOf(keyObj);
				String val=properties.getProperty(key);
				//naive, but should work rather well :-)
				if(key.toLowerCase().contains("password")){
					message(key+"="+"*");
				}
				else{
					message(key+"="+val);
				}
			}
		}
		else {
			//set property
			String[] paramArgs = new String[args.length-1];
			System.arraycopy(args, 1, paramArgs, 0, paramArgs.length);
			properties.putAll(PropertyVariablesResolver.getParameters(paramArgs));
			boolean verbose = UCC.getConsoleLogger().isVerbose();
			verbose = Boolean.parseBoolean(properties.getProperty("verbose", String.valueOf(verbose)));
			UCC.getConsoleLogger().setVerbose(verbose);
		}
	}

	protected void handleSystem(String[] args) throws Exception {
		if(args.length<2)return;
		
		String[] paramArgs = new String[args.length-1];
		System.arraycopy(args, 1, paramArgs, 0, paramArgs.length);
		ProcessBuilder pb = new ProcessBuilder(Arrays.asList(paramArgs));
		pb.inheritIO();
		Process p = pb.start();
		p.waitFor();
	}

	protected void handleUnset(String[] args){
		for(int i=1; i<args.length;i++){
			properties.remove(args[i]);
		}
	}

	protected void handleVersion(){
		UCC.printVersion();
	}

	protected void printShellHelp(){
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

	protected History getHistory(LineReader lineReader) {
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
			verbose("Cannot setup history");
			return null;
		}
	}

	private static Pattern p = Pattern.compile("\"([^\"]*)\"|(\\S+)");
		
	public String[] parseCmdlineWithVars(String cmdArgs) throws IOException {
		String[] args = parseCmdline(cmdArgs);
		for(int i=0; i<args.length; i++) {
			String s = args[i];
			if(s.contains("$")) {
				args[i] = expandVariables(s);
			}
		}
		return args;
	}
	
	private String expandVariables(String var){
		for(String key: properties.stringPropertyNames()) {
			if(!var.contains(key))continue;
			if(var.contains("${"+key+"}")){
				var = var.replace("${"+key+"}", String.valueOf(properties.get(key)));
			}
			else if(var.contains("$"+key)){
				var = var.replace("$"+key, String.valueOf(properties.get(key)));				
			}
		}
		return var;
	}
	
	public static String[] parseCmdline(String cmdArgs) throws IOException {
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
	
	

	public final String getHeader() {
		String lineSep = System.getProperty("line.separator");
		String s = lineSep
				+ " _    _ _   _ _____ _____ ____  _____  ______" + lineSep
				+ "| |  | | \\ | |_   _/ ____/ __ \\|  __ \\|  ____|" + lineSep
				+ "| |  | |  \\| | | || |   | |  | | |__) | |__"+ lineSep
				+ "| |  | | . ` | | || |   | |  | |  _  /|  __|"+ lineSep
				+ "| |__| | |\\  |_| |_ |____ |__| | | \\ \\| |____"+ lineSep
				+ " \\____/|_| \\_|_____\\_____\\____/|_|  \\_\\______|";
		return s;
	}

}
