package eu.unicore.ucc.runner;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
/**
 * @author schuller
 */
public class RunningQueue extends RequestQueue {

	public RunningQueue(String requestDirname) throws IOException{	
		super(requestDirname, true);
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
