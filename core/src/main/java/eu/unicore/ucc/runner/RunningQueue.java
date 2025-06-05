package eu.unicore.ucc.runner;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
/**
 * @author schuller
 */
public class RunningQueue extends RequestQueue {

	public RunningQueue(String requestDirname) throws IOException{	
		super(requestDirname,true);
		if(!getRequestDir().canWrite()) 
			{
				throw new IOException("Directory "+requestDirname+
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
		queued = requestDir.listFiles(getFilter()).length;
		return queued;
	}

	@Override
	protected FileFilter getFilter(){
		return (file)->file.getName().endsWith(".job");
	}
}
