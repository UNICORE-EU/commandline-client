package eu.unicore.ucc.actions.shell;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

import eu.unicore.ucc.Command;
import eu.unicore.ucc.Constants;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.REST;
import eu.unicore.ucc.authn.UCCConfigurationProvider;

/**
 * provide command completion for the "shell" command
 */
public class UCCCompleter implements Completer, Constants {

	final StringsCompleter commands;
	final FileNameCompleter fileNames = new FileNameCompleter();
	final URLCompleter urlCompleter;

	public UCCCompleter(Collection<String> cmds, UCCConfigurationProvider configurationProvider){
		this.commands = new StringsCompleter(cmds);
		this.urlCompleter = configurationProvider!=null ? 
				new URLCompleter(configurationProvider) : null;
	}

	@Override
	public void complete(LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
		int index = line.wordIndex();
		if(index==0) {
			commands.complete(reader, line, candidates);
		}
		else {
			String currentCommand = line.words().get(0);
			boolean stop = false;
			try {
				Command c = UCC.getCommand(currentCommand);
				stop = commandComplete(c, reader, line, candidates);
			}catch(Exception ex) {}
			if(!stop && urlCompleter!=null) {
				stop = urlCompleter.completeURLs(reader, line, candidates);
			}
			if(!stop) {
				fileNames.complete(reader, line, candidates);
			}
		}
	}

	protected boolean commandComplete(Command command, LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
		try{
			String cmdName = command.getName();
			String lastToken = line.word();
			int oldSize = candidates.size();
			if(lastToken.startsWith("-")){
				boolean longOpts = lastToken.startsWith("--");
				while(lastToken.startsWith("-"))lastToken=lastToken.substring(1);
				Collection<Option>opts = command.getOptions().getOptions();
				for(Option o: opts){
					if(longOpts){
						if(o.getLongOpt().startsWith(lastToken)){
							candidates.add(new Candidate("--"+o.getLongOpt()));
						}
					}
					else{
						if(o.getOpt().startsWith(lastToken)){
							candidates.add(new Candidate("-"+o.getOpt()));
						}
					}
				}
				return candidates.size()>oldSize;
			}
			else if ("rest".equals(cmdName) && line.wordIndex()==1) {
				for(String x: REST.cmds)candidates.add(new Candidate(x));
				return true;
			}
			else if ("workflow-control".equals(cmdName) && line.wordIndex()==1) {
				for(String x: new String[] {"abort","resume"})
					candidates.add(new Candidate(x));
				return true;
			}
			if(line.wordIndex()>1 && line.words().get(line.wordIndex()-1).startsWith("-")) {
				String option = line.words().get(line.wordIndex()-1);
				boolean longOpt = option.startsWith("--");
				while(option.startsWith("-"))option=option.substring(1);
				if(option.length()>0) {
					for(Option opt: command.getOptions().getOptions()){
						String optionName = longOpt? opt.getLongOpt(): opt.getOpt();
						if(option.equals(optionName)) {
							return completeOptionArgs(command, opt, reader, line, candidates);
						}
					}
				}
			}
		} catch(Exception e) {}
		return false;
	}

	protected boolean completeOptionArgs(Command command, Option opt, 
			LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
		if(OPT_SITENAME_LONG.equals(opt.getLongOpt())){
			new StringsCompleter(siteNames).complete(reader, line, candidates);
			return true;
		}
		Collection<String> values = command.getAllowedOptionValues(opt.getOpt());
		if(values!=null && values.size()>0) {
			new StringsCompleter(values).complete(reader, line, candidates);
			return true;
		}
		return false;
	}

	static final Set<String> siteNames = new HashSet<>();

	public static void registerSiteName(String siteName) {
		siteNames.add(siteName);
	}

}