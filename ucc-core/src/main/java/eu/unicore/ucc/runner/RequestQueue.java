package eu.unicore.ucc.runner;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import de.fzj.unicore.ucc.util.Queue;

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
		String curr="";	
		File[] files=sort(requestDir.listFiles(getFilter()));
		//fill first (i.e. oldest into queue, if they are not already there
		for (int i=0;i<files.length;i++){
			curr= files[i].getAbsolutePath();
			try{ 
				add(curr);
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
		if (this.getSize()==0) populate();
	}
	
	/**
	 * sort file list so that oldest files are processed first
	 */
	protected File[] sort(File[] in){
		File[] out=in;
		Comparator<File> c=new Comparator<File>(){
			public int compare(File f1, File f2){
				return (int)(f1.lastModified()-f2.lastModified());
			}
		};
		Arrays.sort(out, c);
		return out;
	}
	
	protected FileFilter getFilter(){
		return new FileFilter(){
			public boolean accept (File file){
				return file.getName().endsWith(".u")
                       || file.getName().endsWith(".jsdl") 
                       || file.getName().endsWith(".xml");
			}
		};
	}
 
}