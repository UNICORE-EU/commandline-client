package eu.unicore.ucc.runner;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;

import eu.unicore.ucc.util.Queue;

/**
 * Queue storing incoming requests
 * @author schuller
 */
public class RequestQueue extends Queue {

	protected File requestDir;	

	/**
	 * constructs a new RequestQueue 
	 */
	public RequestQueue(String requestDirname, boolean follow) throws IOException{
		super();
		requestDir=new File(requestDirname);
		if(!requestDir.exists()){
			requestDir.mkdir();
		}
		if(!requestDir.isDirectory() || !requestDir.canRead()){
			throw (new IOException("Cannot read "+requestDirname));
		}
	}

	/**
	 * get the request directory
	 */
	public File getRequestDir(){
		return requestDir;	
	}

	/**
	 * fill stuff into the queue
	 */
	protected synchronized void populate(){
		if(getSize()>0) return;
		File[] files = requestDir.listFiles(getFilter());
		Arrays.sort(files, (f1,f2) -> (int)(f1.lastModified()-f2.lastModified()));
		//fill first (i.e. oldest into queue, if they are not already there
		for (int i=0;i<files.length;i++){
			try{ 
				add(files[i].getAbsolutePath());
			}
			catch(Exception e){
				return; //not a real error
			}
		}
	}

	/**
	 * update the content, if necessary
	 */
	protected void update(){
		if (getSize()==0) populate();
	}

	protected FileFilter getFilter(){
		return (file) -> file.getName().endsWith(".u") || file.getName().endsWith(".json");
	}
}