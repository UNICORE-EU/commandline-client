package eu.unicore.ucc;

import java.io.File;
import java.io.FileInputStream;
import java.text.NumberFormat;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.util.MessageWriter;
import eu.unicore.ucc.helpers.EndProcessingException;
import eu.unicore.ucc.util.PropertyVariablesResolver;
import eu.unicore.util.Log;

/**
 * Base command class. Defines methods for setting up commandline options, logging, etc.
 * 
 * @author schuller
 */
public abstract class Command implements Constants, MessageWriter {

	protected static final Logger logger = Log.getLogger("UCC", Command.class);

	private final UCCOptions options;

	private CommandLine line;

	protected boolean verbose;

	protected long startTime;
	protected long endTime;

	protected boolean timing;

	public static boolean quitAfterPrintingUsage;

	protected File propertiesFile;

	protected Properties properties=null;

	protected NumberFormat numberFormat=UCC.numberFormat;

	/**
	 * output directory
	 */
	protected File output;

	public Command(){
		options=new UCCOptions();
		createOptions();
	}

	public void init(String [] args) throws ParseException{
		if(handleHelp(args)){
			printUsage();
			endProcessing(0);
		}
		CommandLineParser parser = new DefaultParser();
		line = parser.parse( getOptions(), args );
		UCC.setMessageWriter(this);

		if(getCommandLine().hasOption(OPT_VERBOSE)){
			verbose=true;
		}

		loadUserProperties();
	}


	protected void setOutputLocation(){
		verbose("Current directory is <"+new File("").getAbsolutePath()+">");
		String outputLoc=getCommandLine().getOptionValue(OPT_OUTPUT, properties.getProperty(OPT_OUTPUT_LONG));
		if(outputLoc==null)outputLoc=".";
		try{
			output=new File(outputLoc);
			if(!output.exists())output.mkdirs();
			if(!output.isDirectory())throw new IllegalArgumentException("<"+outputLoc+"> is not a directory.");
			verbose("Output goes to <"+outputLoc+">");
		}catch(Exception e){
			error("Problem with <"+outputLoc+">",e);
			endProcessing();
		}
	}


	//handle special case of requesting help (-h or --help option given)
	private boolean handleHelp(String[] args){
		String help="-"+OPT_HELP;
		String help_long="--"+OPT_HELP_LONG;

		for(String arg: args){
			if(help.equals(arg) || help_long.equals(arg))return true;
		}
		return false;
	}

	public String getName(){
		return "<command>";
	}

	public String getArgumentList(){
		return "[ARGS]";
	}

	public String getSynopsis(){
		return "";
	}

	public String getDescription(){
		return "";
	}

	public String getCommandGroup(){
		return "Other";
	}

	public UCCOptions getOptions(){
		return options;
	}

	public CommandLine getCommandLine(){
		return line;
	}

	public void endProcessing(){
		endProcessing(0);
	}

	public void endProcessing(int exitCode){
		if(quitAfterPrintingUsage){
			System.exit(exitCode);
		}
		else throw new EndProcessingException(exitCode);
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
				.build(),
				UCCOptions.GRP_GENERAL);
		if(isTimeable()){
			getOptions().addOption(Option.builder(OPT_TIMING)
					.longOpt(OPT_TIMING_LONG)
					.desc("Timing mode")
					.required(false)
					.build()
					,UCCOptions.GRP_GENERAL);
		}
		
		if(producesOutput()){
			getOptions().addOption(Option.builder(OPT_OUTPUT)
					.longOpt(OPT_OUTPUT_LONG)
					.desc("Directory for any output produced")
					.argName("Output")
					.hasArg()
					.required(false)
					.build()
					,UCCOptions.GRP_GENERAL);
		}
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

	public void printUsage(){
		HelpFormatter formatter = new HelpFormatter();
		String syntax="ucc "+getName()+" [OPTIONS] "+getArgumentList()+"\n"+getSynopsis()+"\n";
		String newLine=System.getProperty("line.separator");
		if(!UCC.mute){
			Options def=options.getDefaultOptions();
			if(def!=null){
				formatter.printHelp(syntax, def);
			}
			else{
				formatter.printHelp(syntax, new Options());
			}
			Options general=options.getGeneralOptions();
			if(general!=null){
				System.out.println();
				formatter.setSyntaxPrefix("General options:");
				formatter.printHelp(" "+newLine, general);
			}
			Options security=options.getSecurityOptions();
			if(security!=null){
				System.out.println();
				formatter.setSyntaxPrefix("Security options:");
				formatter.printHelp(" "+newLine, security);
			}
			Options vo=options.getVOOptions();
			if(vo!=null){
				System.out.println();
				formatter.setSyntaxPrefix("VO-related options:");
				formatter.printHelp(" "+newLine, vo);
			}
		}
	}

	/**
	 * process this command<br/>
	 * NOTE: Subclasses <em>must</em> call super.process() to ensure
	 * proper initialisation, preferences handling etc. 
	 */
	public void process(){
		if(getCommandLine().hasOption(OPT_HELP)){
			printUsage();
			endProcessing();
		}
		verbose = getBooleanOption(OPT_VERBOSE_LONG, OPT_VERBOSE);
		timing = getBooleanOption(OPT_TIMING_LONG, OPT_TIMING);
		if(timing){
			startTime=System.currentTimeMillis();
			verbose("Timing mode.");
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
			message("Time: "+numberFormat.format(duration)+" sec.");
		}
	}

	/**
	 * read user's properties file
	 */
	protected void loadUserProperties(){
		if(properties!=null)return;
		verbose("UCC "+UCC.getVersion());

		properties=new Properties();
		boolean userSpecified=false;
		CommandLine line=getCommandLine();
		String defaultProps=System.getProperty("user.home")+File.separator+".ucc"+File.separator+"preferences";
		String props=System.getProperty("ucc.preferences",defaultProps);

		if(propertiesFile==null){
			if(line.hasOption(OPT_PROPERTIES)){
				props=line.getOptionValue(OPT_PROPERTIES);
				verbose("Properties file: <"+props+">");
				userSpecified=true;
			}
			propertiesFile=new File(props);
		}
		if(propertiesFile.exists()){
			verbose("Reading properties file <"+props+">");
			try{
				properties.load(new FileInputStream(propertiesFile));
			}catch(Exception e){
				error("Could not read from <"+props+">.",e);
				endProcessing(1);
			}
		}
		else{
			if(userSpecified){
				error("Properties file <"+props+"> does not exist.",null);
				endProcessing(1);
			}
			else{
				verbose("No properties file found at <"+props+">");	
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
			verbose("WARN: deprecated config file property 'authenticationMethod', "
					+ "updating to '"+OPT_AUTHN_METHOD_LONG+"'");
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
			error("Can't parse supplied value for option <"+longForm+">, using default <"+defaultValue+">",e);
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

	@Override
	public void verbose(String message){
		if(verbose)System.out.println("[ucc "+getName()+"] "+message);
		logger.debug(message);
	}
	
	@Override
	public void message(String message){
		if(UCC.mute)return;
		System.out.println(message);
		logger.debug(message);
	}

	@Override
	public void error(String message, Throwable cause){
		System.err.println(message);
		if(cause!=null){
			System.err.println("The root error was: "+Log.getDetailMessage(cause));
			if(verbose)cause.printStackTrace();
			else{
				System.err.println("Re-run in verbose mode (-v) to see the full error stack trace.");
			}
		}
		logger.debug(message, cause);
	}

	@Override
	public boolean isVerbose(){
		return verbose;
	}
	
	public void setVerbose(boolean verbose){
		this.verbose = verbose;
	}
	
	public void setOutputDirectory(File output){
		this.output=output;
	}
}
