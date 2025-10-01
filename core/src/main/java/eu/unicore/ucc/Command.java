package eu.unicore.ucc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.logging.log4j.Logger;

import eu.unicore.ucc.helpers.ConsoleLogger;
import eu.unicore.ucc.util.PropertyVariablesResolver;
import eu.unicore.util.Log;

/**
 * Base command class. Defines methods for setting up commandline options, logging, etc.
 * 
 * @author schuller
 */
public abstract class Command implements Constants {

	protected static final Logger logger = Log.getLogger("UCC", Command.class);

	private final UCCOptions options;

	private CommandLine line;

	protected long startTime, endTime;

	protected boolean timing;

	public static boolean quitAfterPrintingUsage;

	protected File propertiesFile;

	protected Properties properties = null;

	protected NumberFormat numberFormat = UCC.numberFormat;

	protected static ConsoleLogger console = UCC.console;

	/**
	 * output directory
	 */
	protected File output;

	public Command(){
		options=new UCCOptions();
		createOptions();
	}

	/**
	 * handles --help, parses commandline args, loads properties file
	 * returns <code>false</code> if command processing should not continue
	 */
	public boolean init(String [] args) throws Exception {
		if(handleHelp(args)){
			printUsage();
			if(quitAfterPrintingUsage)System.exit(0);
			return false;
		}
		CommandLineParser parser = new DefaultParser();
		line = parser.parse( getOptions(), args );
		if(getCommandLine().hasOption(OPT_VERBOSE)){
			console.setVerbose(true);
		}
		loadUserProperties();
		return true;
	}

	protected void setOutputLocation() throws Exception {
		console.debug("Current directory is <{}>", new File("").getAbsolutePath());
		String outputLoc = getCommandLine().getOptionValue(OPT_OUTPUT, properties.getProperty(OPT_OUTPUT_LONG));
		if(outputLoc==null)outputLoc = ".";
		output = new File(outputLoc);
		if(!output.exists())output.mkdirs();
		if(!output.isDirectory())throw new IllegalArgumentException("<"+outputLoc+"> is not a directory.");
		console.debug("Output goes to <{}>", outputLoc);
	}

	// handle special case of requesting help (-h or --help option given)
	private boolean handleHelp(String[] args){
		String help="-"+OPT_HELP;
		String help_long="--"+OPT_HELP_LONG;

		for(String arg: args){
			if(help.equals(arg) || help_long.equals(arg))return true;
		}
		return false;
	}

	public abstract String getName();

	public String getArgumentList(){
		return "";
	}

	public abstract String getSynopsis();

	public abstract String getDescription();

	public String getCommandGroup(){
		return CMD_GRP_OTHER;
	}

	public UCCOptions getOptions(){
		return options;
	}

	public CommandLine getCommandLine(){
		return line;
	}

	/**
	 * add options understood by the command
	 */
	protected void createOptions(){
		getOptions().addOption(new Option(OPT_HELP,OPT_HELP_LONG,false,"Print this help message")
		,UCCOptions.GRP_GENERAL);
		getOptions().addOption(Option.builder(OPT_VERBOSE)
				.longOpt(OPT_VERBOSE_LONG)
				.desc("Verbose mode")
				.argName("Verbose")
				.required(false)
				.get(),
				UCCOptions.GRP_GENERAL);
		if(isTimeable()){
			getOptions().addOption(Option.builder(OPT_TIMING)
					.longOpt(OPT_TIMING_LONG)
					.desc("Timing mode")
					.required(false)
					.get()
					,UCCOptions.GRP_GENERAL);
		}
		if(producesOutput()){
			getOptions().addOption(Option.builder(OPT_OUTPUT)
					.longOpt(OPT_OUTPUT_LONG)
					.desc("Directory for any output produced")
					.argName("Output")
					.hasArg()
					.required(false)
					.get()
					,UCCOptions.GRP_GENERAL);
		}
	}

	/**
	 * for use by the completor: returns possible values for
	 * the given option
	 * @param option - short option
	 */
	public Collection<String> getAllowedOptionValues(String option) {
		return null;
	}

	/**
	 * Function that returns true if this command potentially produces files
	 * By default always returns true; override if needed.
	 */
	protected boolean producesOutput(){
		return true;
	}

	/**
	 * Function that returns true if this command can be timed
	 * By default always returns true; override if needed.
	 */
	protected boolean isTimeable(){
		return true;
	}

	public void printUsage() throws IOException {
		HelpFormatter.Builder b = HelpFormatter.builder();
		b.setShowSince(false);
		HelpFormatter formatter = b.get();
		String syntax="ucc "+getName()+" [OPTIONS] "+getArgumentList()+"\n"+getSynopsis()+"\n";
		List<Option> def = options.getDefaultOptions();
		formatter.printHelp(syntax, "", def, "", false);
		List<Option> general = options.getGeneralOptions();
		formatter.setSyntaxPrefix("General options:");
		formatter.printHelp(" "+_newline, "", general, "", false);
		List<Option> security = options.getSecurityOptions();
		formatter.setSyntaxPrefix("Security options:");
		formatter.printHelp(" "+_newline, "", security, "", false);
	}

	/**
	 * process this command<br/>
	 * NOTE: Subclasses <em>must</em> call super.process() to ensure
	 * proper initialisation, preferences handling etc. 
	 */
	public void process() throws Exception {
		timing = getBooleanOption(OPT_TIMING_LONG, OPT_TIMING);
		if(timing){
			startTime=System.currentTimeMillis();
			console.debug("Timing mode.");
		}
		setOutputLocation();
	}

	/**
	 * common post processing. If overriding, make sure
	 * to call super.postProcess()
	 */
	public void postProcess(){
		if(timing){
			endTime=System.currentTimeMillis();
			float duration=(endTime-startTime)/1000;
			console.info("Time: {} sec.", numberFormat.format(duration));
		}
	}

	/**
	 * read user's properties file
	 */
	protected void loadUserProperties() throws Exception {
		if(properties!=null)return;
		properties=new Properties();
		boolean userSpecified=false;
		CommandLine line=getCommandLine();
		String defaultProps=System.getProperty("user.home")+File.separator+".ucc"+File.separator+"preferences";
		String props=System.getProperty("ucc.preferences",defaultProps);
		if(propertiesFile==null){
			if(line.hasOption(OPT_PROPERTIES)){
				props=line.getOptionValue(OPT_PROPERTIES);
				console.debug("Properties file: <{}>", props);
				userSpecified=true;
			}
			propertiesFile=new File(props);
		}
		if(propertiesFile.exists()){
			console.debug("Reading properties file: <{}>", props);
			try(FileInputStream fis = new FileInputStream(propertiesFile)){
				properties.load(fis);
			}
		}
		else{
			if(userSpecified){
				throw new Exception("Properties file <"+props+"> does not exist.");
			}
			else{
				console.debug("No properties file found at <{}>", props);	
			}
		}
		// set default session ID file, if not set
		if(properties.getProperty(SESSION_ID_FILEKEY)==null){
			String defaultSessionsFile=System.getProperty("user.home")+File.separator+".ucc"
					+File.separator+SESSION_ID_FILEKEY;
			properties.put(SESSION_ID_FILEKEY, defaultSessionsFile);
		}
		PropertyVariablesResolver.substituteVariables(properties, propertiesFile);
		checkDeprecation();
	}

	protected void checkDeprecation() {
		if(properties==null)throw new IllegalStateException();
		String authMethod = properties.getProperty("authenticationMethod");
		if(authMethod!=null && properties.getProperty(OPT_AUTHN_METHOD_LONG)==null) {
			console.debug("WARN: deprecated config file property 'authenticationMethod', "
					+ "updating to '{}'", OPT_AUTHN_METHOD_LONG);
			properties.setProperty(OPT_AUTHN_METHOD_LONG, authMethod);
			properties.remove("authenticationMethod");
		}
	}

	public void setProperties(Properties properties){
		this.properties=properties;
	}

	public void setPropertiesFile(File propertiesFile){
		this.propertiesFile=propertiesFile;
	}

	public Properties getProperties(){
		return properties;
	}

	/**
	 * gets a numeric option value
	 * 
	 * @param longForm - long option name
	 * @param shortForm - short option name
	 * @param defaultValue - default value if option is not given
	 */
	protected int getNumericOption(String longForm, String shortForm, int defaultValue){
		String val=getCommandLine().getOptionValue(shortForm, properties.getProperty(longForm, ""+defaultValue));
		try{
			return Integer.parseInt(val);
		}catch(Exception e){
			console.error(e, "Can't parse supplied value for option <{}>, using default <{}>", longForm, defaultValue);
			return defaultValue;
		}
	}

	/**
	 * gets a boolean option (the presence of a flag such as '-v' for verbose)
	 * returns 'true' if flag is present, false otherwise<br/>
	 * If not present on the commandline, the properties are checked as well.
	 * For example, to switch on "verbose mode", you can either specify '-v' on the commandline,
	 * or "verbose=true" in the properties file.
	 * 
	 * @param longForm -  long option name
	 * @param shortForm - short option name
	 */
	protected boolean getBooleanOption(String longForm, String shortForm){
		String val;
		if(getCommandLine().hasOption(shortForm)){
			val="true";
		}
		else{
			val=properties.getProperty(longForm, "false");
		}
		return Boolean.parseBoolean(val);
	}

	/**
	 * gets an option from the command line or, if not given, from the properties file
	 * 
	 * @param longForm -  long option name
	 * @param shortForm - short option name
	 */
	public String getOption(String longForm, String shortForm){
		return getOption(longForm, shortForm, null);
	}

	/**
	 * gets an option from the command line or, if not given, from the properties file
	 * 
	 * @param longForm -  long option name
	 * @param shortForm - short option name
	 * @param defaultValue - the default value to return if the option is not given 
	 */
	public String getOption(String longForm, String shortForm, String defaultValue){
		String val;
		if(getCommandLine().hasOption(shortForm)){
			val=getCommandLine().getOptionValue(shortForm);
		}
		else{
			val=properties.getProperty(longForm,defaultValue);
		}
		return val;
	}

	public void setOutputDirectory(File output){
		this.output=output;
	}
}
