package eu.unicore.ucc.actions.shell;

import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.Option;
import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

import eu.unicore.ucc.Command;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.UCCConfigurationProvider;

/**
 * provide command completion for the "shell" command
 */
public class UCCCompleter implements Completer {

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
				return longOpts;
			}
			else if ("rest".equals(cmdName) &&line.wordIndex()==1) {
				for(String x: new String[] {"GET","PUT","POST","DELETE"})
					candidates.add(new Candidate(x));
				return true;
			}
		} catch(Exception e) {}
		return false;
	}

}