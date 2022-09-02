package eu.unicore.ucc.actions;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.cli.Option;
import org.codehaus.groovy.control.CompilerConfiguration;

import eu.unicore.ucc.helpers.Base;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

/**
 * run a Groovy script in a ucc context
 * @author schuller
 */
public class Groovy extends ActionBase{

	protected String expression;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_GROOVYSCRIPT)
				.longOpt(OPT_GROOVYSCRIPT_LONG)
				.desc("File containing the Groovy script")
				.argName("Script")
				.hasArg()
				.required(false)
				.build());
		
		getOptions().addOption(Option.builder(OPT_GROOVYEXPRESSION)
				.longOpt(OPT_GROOVYEXPRESSION_LONG)
				.desc("the Groovy expression")
				.argName("Expression")
				.hasArg()
				.required(false)
				.build());
		
	}

	@Override
	public void process() {
		super.process();
		CompilerConfiguration conf=new CompilerConfiguration();
		conf.setScriptBaseClass(Base.class.getName());
		//inject context for the script
		GroovyShell shell=new GroovyShell(conf);
		Binding binding = shell.getContext();
		binding.setVariable("registry", registry);
		binding.setVariable("configurationProvider", configurationProvider);
		binding.setVariable("auth", configurationProvider.getRESTAuthN());
		binding.setVariable("registryURL", registryURL);
		binding.setVariable("properties", properties);
		binding.setVariable("options", getOptions());
		binding.setVariable("commandLine", getCommandLine());
		binding.setVariable("messageWriter", this);
		if(getCommandLine().hasOption(OPT_GROOVYEXPRESSION)){
			expression=getCommandLine().getOptionValue(OPT_GROOVYEXPRESSION);
		}else if (getCommandLine().hasOption(OPT_GROOVYSCRIPT)){
			try{
				expression=readFile(getCommandLine().getOptionValue(OPT_GROOVYSCRIPT));
			}catch(Exception e){
				error("Can't read file, or file not given.",e);
				endProcessing(ERROR_CLIENT);
			}
		}
		else{
			getFallbackGroovyExpression();
		}
		shell.evaluate(expression);
	}
	
	/**
	 * if no expression or file is given on the commandline, this method should 
	 * return an expression to execute. The default implementation does not do this, but
	 * prints usage info
	 */
	public void getFallbackGroovyExpression(){
		printUsage();
	}
	
	private String readFile(String name)throws Exception{
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		InputStream fis=new BufferedInputStream(new FileInputStream(new File(name).getAbsolutePath()));
		try{
			int b=0;
			while((b=fis.read())!=-1){
				bos.write(b);
			}
			return bos.toString();
		}finally{
			try{
				fis.close();
			}catch(Exception e){}
		}
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

}
