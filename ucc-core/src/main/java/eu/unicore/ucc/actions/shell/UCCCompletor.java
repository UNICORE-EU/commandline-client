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

import de.fzj.unicore.ucc.Command;
import de.fzj.unicore.ucc.UCC;

/**
 * provide command completion for the "shell" command
 */
public class UCCCompletor implements Completer {

	final StringsCompleter commands;
	final FileNameCompleter fileNames = new FileNameCompleter();

	public UCCCompletor(Collection<String> cmds){
		this.commands = new StringsCompleter(cmds);
	}

	public void complete(LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
		int index = line.wordIndex();
		if(index==0) {
			commands.complete(reader, line, candidates);
		}
		else {
			String currentCommand = line.words().get(0);
			boolean tryFiles = true;
			try {
				Command c = UCC.getCommand(currentCommand);
				tryFiles = commandComplete(c, reader, line, candidates);
			}catch(Exception ex) {}
			if(tryFiles)fileNames.complete(reader, line, candidates);
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
			}
			else if ("rest".equals(cmdName) &&line.wordIndex()==1) {
				for(String x: new String[] {"GET","PUT","POST","DELETE"})
					candidates.add(new Candidate(x));
				return false;
			}
		} catch(Exception e) {}
		return true;
	}

}