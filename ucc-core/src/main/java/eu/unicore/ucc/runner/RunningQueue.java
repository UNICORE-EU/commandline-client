package eu.unicore.ucc.runner;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
/**
 * @author schuller
 */
public class RunningQueue extends RequestQueue {
	
	/**
	 * constructs a new RunningQueue 
	 */
	public RunningQueue(String requestDirname) throws IOException{	
		super(requestDirname,true);
		if(!getRequestDir().canWrite()) 
			{
				throw new IOException("Fatal: directory "+requestDirname+
					"must be writable.");
			}
	}
	/**
	 * ask whether another item can be added into the queue 
	 * @return true if there still is some space left
	 */
	public boolean canAdd(){
		if(limit!=-1 && length()>=limit) return false;
		else return true;
	}
	
	/** 
	 * this will add a "running" request (in the form of a filename of a
	 * job id file)
	 */
	public void add(String o) throws Exception {
		super.add(o,new File(o).lastModified());
	}	

	@Override
	public int length(){
		File[] files=requestDir.listFiles(getFilter());
		queued=files.length; //update the number of queued things
		return queued;
	}
	
	@Override
	protected FileFilter getFilter(){
		return new FileFilter(){
			public boolean accept (File file){
				return file.getName().endsWith(".job");
			}
		};
	}
}
