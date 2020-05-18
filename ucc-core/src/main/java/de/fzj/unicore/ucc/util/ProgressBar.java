package de.fzj.unicore.ucc.util;

import de.fzj.unicore.uas.fts.ProgressListener;
import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.uas.util.UnitParser;
import jline.Terminal;
import jline.TerminalFactory;
import jline.console.ConsoleReader;

/**
 * console progress bar using jline
 * 
 * @author schuller
 */
public class ProgressBar implements ProgressListener<Long> {

	private Terminal terminal=null;	
	private ConsoleReader reader=null;
	private MessageWriter msg=null;
	private long size=-1;
	private long have=0;
	private long startedAt=0;
	private final UnitParser rateParser=UnitParser.getCapacitiesParser(1);
	private String sizeDisplay;
	
	//bytes per second
	private double rate=0;
	
	private final String identifier;

	//for displaying spinning thingy if size is unknown
	private final char[] x=new char[]{'|','/','-','\\'};
	private int index=0;

	/**
	 * create a new progress bar
	 * @param msg - message writer
	 */
	public ProgressBar(MessageWriter msg){
		this("",-1,msg);
	}

	/**
	 * 
	 * @param identifier - fixed ID, e.g. file name, to display
	 * @param size - if this is non-positive, a "spinning" progress indicator will be displyoed
	 * @param msg - message writer for logging
	 */
	public ProgressBar(String identifier,long size, MessageWriter msg){
		this.identifier=identifier;
		this.msg=msg;
		startedAt=System.currentTimeMillis();
		try {
			
			TerminalFactory.configure(TerminalFactory.NONE);
			
			terminal = TerminalFactory.get();
			
			reader = new ConsoleReader();
	
		} catch (Exception e) {
			msg.error("Could not setup jline console output: "+e,null);
			terminal = null;
		}
		setSize(size);
	}

	public void setSize(long size){
		this.size=size;
		this.sizeDisplay=rateParser.getHumanReadable(size);
	}
	
	public void updateTotal(long total){
		if(terminal==null || total<0)return;
		updateRate();
		have=total;
		output();
	}

	public void update(long amount) {
		if(terminal==null || amount<=0)return;
		have+=amount;
		updateRate();
		output();
	}

	/*
	 * update transfer rate in bytes/s
	 * @param amount - transfer amount in bytes
	 */
	protected void updateRate(){
		rate=1000*(double)have/(System.currentTimeMillis()-startedAt);
	}
	
	protected void output(){
		StringBuilder sb=new StringBuilder();
		if(size>0){
			long progress=have*100/size;
			sb.append(String.format("%3d%%  %s ",progress, sizeDisplay));
		}
		else{
			//for unknown size, just display a 'rotating' thingy
			sb.append(x[index]);
			index++;
			if(index==x.length)index=0;
		}
		
		//append rate
		if(rate>0){
			sb.append(String.format("%sB/s", rateParser.getHumanReadable(rate)));
		}
		
		//compute maximum with of identifier printout
		int w=getTerminalWidth();
		int max=w-sb.length()-5;
		if(max>0){
			sb.insert(0, String.format("%-"+max+"s ", identifier));
		}
		
		try {
			reader.getCursorBuffer().clear();
			reader.getCursorBuffer().write(sb.toString());
			reader.setCursorPosition(w);
			reader.redrawLine();
			reader.flush();
			
		}
		catch (Exception e) {
			msg.error("Could not output to jline console: "+e,null);
			terminal = null;
		}
	}

	private int width=0;
	
	private int getTerminalWidth(){
		if(width==0){
			width=terminal.getWidth();
		}
		return width;
	}
	
	public void notifyProgress(Long amount) {
		if(amount!=null){
			update(amount);
		}
	}

	public boolean isCancelled() {
		return false;
	}

	public void finish(){
		if(size>0){
			have=size;
		}
		else{
			setSize(have);
		}
		output();
		System.out.println();
		// TODO: Review me. is the ConsoleReader further required?
		if(reader != null) reader.shutdown();
	}


}
