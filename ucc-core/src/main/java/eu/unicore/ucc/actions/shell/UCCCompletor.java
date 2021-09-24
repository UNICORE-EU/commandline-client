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
			try {
				Command c = UCC.getCommand(currentCommand);
				commandComplete(c, reader, line, candidates);
			}catch(Exception ex) {}
			fileNames.complete(reader, line, candidates);
		}
	}

	@SuppressWarnings("unchecked")
	protected void commandComplete(Command command, LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
		try{
			//String[] parts=buffer.split(" +");
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
		} catch(Exception e) {}
	}

}