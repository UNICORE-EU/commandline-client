package eu.unicore.ucc.actions.shell;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Option;

import de.fzj.unicore.ucc.Command;
import de.fzj.unicore.ucc.UCC;
import jline.console.completer.Completer;

/**
 * provide basic command completion for the "shell" command
 */
public class UCCCompletor implements Completer{

	final String[] cmds;

	public UCCCompletor(String[] cmds){
		this.cmds=cmds;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int complete(String buffer, int cursor, List completions) {
		completions.clear();
		String[] parts=buffer.split(" +");
		if(cursor<=parts[0].length()){
			//complete UCC command name
			return completeUCCCommand(parts, completions);
		}
		else {
			try{
				Command command=UCC.getCommand(parts[0]);
				return completeCommand(command, buffer, cursor, completions);
			}catch(Exception ex){
				// can happen if the first token is not an existing command
				// try to complete as a filename
				return completeFilename(buffer, cursor, completions);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int completeUCCCommand(String[] parts, List completions) {
		String cmd=parts[0];
		completions.clear();
		List<String>matches=new ArrayList<String>();
		for(String c: cmds){
			if (c.startsWith(cmd)){
				matches.add(c);
			}
		}
		if(matches.size()>1){
			completions.addAll(matches);
		}
		else if(matches.size()>0){
			completions.add(matches.get(0)+" ");
		}
		Collections.sort(completions);
		return 0;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected int completeCommand(Command command, String buffer, int cursor, List completions){
		try{
			String[] parts=buffer.split(" +");
			String lastToken=parts[parts.length-1];
			boolean empty=buffer.endsWith(" ");
			String base=empty ? buffer : buffer.substring(0, buffer.lastIndexOf(lastToken));
			if(!empty && lastToken.startsWith("-")){
				boolean longOpts=lastToken.startsWith("--");
				while(lastToken.startsWith("-"))lastToken=lastToken.substring(1);
				Collection<Option>opts=command.getOptions().getOptions();
				for(Option o: opts){
					if(longOpts){
						if(o.getLongOpt().startsWith(lastToken)){
							completions.add(base+"--"+o.getLongOpt()+" ");
						}
					}
					else{
						if(o.getOpt().startsWith(lastToken)){
							completions.add(base+"-"+o.getOpt()+" ");
						}
					}
				}
				return 0;
			}
			else{
				//try to complete as a filename
				File parent=null;
				String pattern=null;
				if(empty){
					parent=new File(".");
					pattern="";
				}
				else{
					File f=new File(lastToken);
					parent=f.getParentFile();
					pattern=f.getName();
					if(f.isDirectory() && lastToken.endsWith("/")){
						parent=f;
						pattern="";
					}
				}
				return completeFileName(base,parent,pattern,cursor,completions);
			}
		}catch(Exception ex){/*ignore*/}
		return -1;
	}

	@SuppressWarnings("rawtypes")
	protected int completeFilename(String buffer, int cursor, List completions){
		try{
			completions.clear();
			String[] parts=buffer.split(" +");
			String lastToken=parts[parts.length-1];
			boolean empty=buffer.endsWith(" ");
			String base=empty ? buffer : buffer.substring(0, buffer.lastIndexOf(lastToken));
			File parent=null;
			String pattern=null;
			if(empty){
				parent=new File(".");
				pattern="";
			}
			else{
				File f=new File(lastToken);
				parent=f.getParentFile();
				pattern=f.getName();
				if(f.isDirectory() && lastToken.endsWith("/")){
					parent=f;
					pattern="";
				}
			}
			return completeFileName(base,parent,pattern,cursor,completions);
		}catch(Exception ex){/*ignore*/}
		return -1;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int completeFileName(String base, File parent, String filePattern, int cursor, List completions){
		try{
			File f=new File(filePattern);
			final String fileName=f.getName();
			if(parent==null)parent=new File(".");
			
			FilenameFilter ff=new FilenameFilter(){
				public boolean accept(File dir, String name) {
					return name.startsWith(fileName);
				}
			};
			
			File[]children=parent.listFiles(ff);
			for(File child: children){
				String completion=f.isAbsolute()?child.getAbsolutePath():child.getPath();
				String compl=base+completion;
				if(children.length==1){
					compl=base+completion+(child.isDirectory()? "/" : " ");
				}
				completions.add(compl);
			}

		}catch(Exception ex){/*ignore*/}
		return 0;
	}

}
