package eu.unicore.ucc.actions;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilerConfiguration;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.helpers.Base;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

/**
 * run a Groovy script in a ucc context
 *
 * @author schuller
 */
public class Groovy extends ActionBase{

	private String expression;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_GROOVYSCRIPT)
				.longOpt(OPT_GROOVYSCRIPT_LONG)
				.desc("File containing the Groovy script")
				.argName("Script")
				.hasArg()
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_GROOVYEXPRESSION)
				.longOpt(OPT_GROOVYEXPRESSION_LONG)
				.desc("the Groovy expression")
				.argName("Expression")
				.hasArg()
				.required(false)
				.get());
		
	}

	@Override
	public void process() throws Exception {
		super.process();
		CompilerConfiguration conf = new CompilerConfiguration();
		conf.setScriptBaseClass(Base.class.getName());
		//inject context for the script
		GroovyShell shell = new GroovyShell(conf);
		Binding binding = shell.getContext();
		binding.setVariable("registry", registry);
		binding.setVariable("configurationProvider", configurationProvider);
		binding.setVariable("auth", configurationProvider.getRESTAuthN());
		binding.setVariable("registryURL", registryURL);
		binding.setVariable("properties", properties);
		binding.setVariable("options", getOptions());
		binding.setVariable("commandLine", getCommandLine());
		binding.setVariable("console", UCC.console);
		if(getCommandLine().hasOption(OPT_GROOVYEXPRESSION)){
			expression = getCommandLine().getOptionValue(OPT_GROOVYEXPRESSION);
		}else if (getCommandLine().hasOption(OPT_GROOVYSCRIPT)){
			expression = readFile(getCommandLine().getOptionValue(OPT_GROOVYSCRIPT));
		}
		else{
			printUsage();
		}
		shell.evaluate(expression);
	}

	private String readFile(String name)throws Exception{
		return FileUtils.readFileToString(new File(name), "UTF-8");
	}

	@Override
	public String getName() {
		return "run-groovy";
	}

	@Override
	public String getSynopsis() {
		return "Runs a Groovy script file (specified using '-f') or expression (specified using '-e').";
	}

	@Override
	public String getDescription(){
		return "run a Groovy script";
	}

	@Override
	public String getArgumentList(){
		return "<script_args>";
	}

	@Override
	protected boolean requireRegistry() {
		return false;
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_UTILITY;
	}
}
