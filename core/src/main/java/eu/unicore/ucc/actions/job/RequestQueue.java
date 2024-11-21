package eu.unicore.ucc.actions.job;
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

	private final File requestDir;	

	/**
	 * constructs a new RequestQueue 
	 */
	public RequestQueue(String requestDirname, boolean follow) throws IOException{
		super();
		requestDir = new File(requestDirname);
		if(!requestDir.exists()){
			requestDir.mkdir();
		}
		if(!requestDir.isDirectory() || !requestDir.canRead()){
			throw new IOException("Cannot read "+requestDirname);
		}
	}

	/**
	 * fill stuff into the queue
	 */
	private synchronized void populate(){
		if(getSize()>0) return;
		File[] files = requestDir.listFiles(getFilter());
		// sort so that oldest files are processed first
		Arrays.sort(files, (f1,f2)->{
			return (int)(f1.lastModified()-f2.lastModified());
		});
		for (File f: files){
			try{ 
				add(f.getAbsolutePath());
			}
			catch(Exception e){
				return; //not a real error
			}
		}
	}

	@Override
	protected void update(){
		if (this.getSize()==0) populate();
	}

	private FileFilter getFilter(){
		return new FileFilter(){
			public boolean accept (File file){
				return file.getName().endsWith(".u")
                       || file.getName().endsWith(".json");
			}
		};
	}
}