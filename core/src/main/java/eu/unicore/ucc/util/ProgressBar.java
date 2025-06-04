package eu.unicore.ucc.util;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import eu.unicore.uas.fts.ProgressListener;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;

/**
 * console progress bar using jline
 * 
 * @author schuller
 */
public class ProgressBar implements ProgressListener<Long> {

	private Terminal terminal;
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
	 */
	public ProgressBar(){
		this("",-1);
	}

	/**
	 * @param identifier - fixed ID, e.g. file name, to display
	 * @param size - if this is non-positive, a "spinning" progress indicator will be displayed
	 */
	public ProgressBar(String identifier, long size){
		this.identifier=identifier;
		startedAt=System.currentTimeMillis();
		try {
			terminal = TerminalBuilder.terminal();
		} catch (Exception e) {
			UCC.console.error(e, "Could not setup jline console output");
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
	private void updateRate(){
		rate=1000*(double)have/(System.currentTimeMillis()-startedAt);
	}

	private void output(){
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
		int max = getTerminalWidth()-5-sb.length();
		sb.insert(0, String.format("%-"+max+"s ", identifier));
		try {
			terminal.puts(Capability.carriage_return);
			terminal.writer().write(sb.toString());
			terminal.flush();
		}
		catch (Exception e) {
			UCC.console.error(e, "Could not output to jline console");
			terminal = null;
		}
	}

	private int width=0;

	private int getTerminalWidth(){
		if(width==0){
			width = Math.max(80, terminal.getWidth());
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
		try{
			terminal.close();
		}catch(Exception ex) {}
	}

}
