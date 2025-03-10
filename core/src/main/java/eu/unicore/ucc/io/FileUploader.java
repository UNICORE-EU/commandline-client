package eu.unicore.ucc.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.client.data.UFTPConstants;
import eu.unicore.client.data.UFTPFileTransferClient;
import eu.unicore.uas.fts.FiletransferOptions;
import eu.unicore.uas.fts.FiletransferOptions.IMonitorable;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.ProgressBar;

/**
 * upload local file(s) to a remote location
 *
 * @author schuller
 */
public class FileUploader extends FileTransferBase {

	public FileUploader(File baseDirectory, String from, String to, Mode mode)throws FileNotFoundException{
		this(baseDirectory, from, to, mode, true);
	}

	public FileUploader(File baseDirectory, String from, String to, Mode mode, boolean failOnError)throws FileNotFoundException{
		this.to=to;
		this.from = resolveFromBaseDir(from, baseDirectory);
		this.mode=mode;
		this.failOnError=failOnError;
		checkOK();
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	@Override
	public void perform(StorageClient sms)throws Exception {
		assertReady(sms);
		File fileSpec = new File(from);
		boolean hasWildCards = false;
		boolean isDirectory = fileSpec.isDirectory();
		File[] fileset = null;

		if(!isDirectory){
			hasWildCards = hasWildCards(fileSpec);
		}

		chosenProtocol = determineProtocol(preferredProtocol, sms);
		Map<String,String>extraParameters = makeExtraParameters(chosenProtocol);

		if(!hasWildCards && !isDirectory){
			//single regular file
			if(to==null)to = "/";
			if(isValidDirectory(to, sms)){
				to = to+"/"+fileSpec.getName();
			}
			uploadFile(fileSpec, to, sms, chosenProtocol, extraParameters);
			return;
		}

		//handle wildcards or directory
		if(hasWildCards){
			fileset=resolveWildCards(fileSpec);
		}
		else{
			fileset=fileSpec.listFiles();
		}
		if(to==null)to = "/";
		if(!isValidDirectory(to, sms)){
			throw new IOException("The specified remote target '"+to+"' is not a directory");
		}
		String target=isDirectory?to+fileSpec.getName():to;
		sms.mkdir(target);
		uploadFiles(fileset,target,sms,chosenProtocol,extraParameters);
	}

	/**
	 * upload a set of files to a remote directory (which must exist)
	 * 
	 * @param files
	 * @param remoteDirectory
	 * @param sms
	 * @param protocol
	 * @param extraParameters
	 * @param msg
	 * @throws Exception
	 */
	private void uploadFiles(File[]files, String remoteDirectory, StorageClient sms, String protocol, 
			Map<String,String>extraParameters) throws Exception {
		for(File localFile: files){
			String target=remoteDirectory+"/"+localFile.getName();
			if(localFile.isDirectory()){
				if(!recurse){
					UCC.console.verbose("Skipping directory {}", localFile.getAbsolutePath());
				}else{
					File[] fileset=localFile.listFiles();
					sms.mkdir(target);
					uploadFiles(fileset,target,sms,protocol,extraParameters);
				}
			}else{
				uploadFile(localFile,target,sms,protocol,extraParameters);
			}
		}
	}

	/**
	 * uploads a single regular file
	 * 
	 * @param localFile
	 * @param remotePath
	 * @param sms
	 * @param protocol
	 * @param extraParameters
	 * @param msg
	 * @throws Exception
	 */
	private void uploadFile(File localFile, String remotePath, StorageClient sms, String protocol, 
			Map<String,String>extraParameters) throws Exception {
		long startTime=System.currentTimeMillis();
		if(remotePath==null){
			remotePath="/"+localFile.getName();
		}
		else if(remotePath.endsWith("/")){
			remotePath+=localFile.getName();
		}
		UCC.console.verbose("Uploading local file '{}' -> '{}/files/{}'",
				localFile.getAbsolutePath(), sms.getEndpoint().getUrl(), remotePath);
		FiletransferClient ftc = null;
		try(FileInputStream is = new FileInputStream(localFile.getAbsolutePath())){
			boolean resume = Mode.RESUME.equals(mode);
			boolean append = Mode.APPEND.equals(mode) || resume;
			if(resume){
				setupOffsetForResume(remotePath, sms);
			}
			ftc = sms.createImport(remotePath, append, localFile.length(), protocol, extraParameters);
			configure(ftc, extraParameters);
			if(append) {
				if(ftc instanceof FiletransferOptions.IAppendable) {
					((FiletransferOptions.IAppendable)ftc).setAppend();
				}else {
					throw new Exception("Append is not supported by protocol <"+protocol+">");
				}	
			}
			String url=ftc.getEndpoint().getUrl();
			UCC.console.verbose("File transfer URL: {}", url);
			ProgressBar p=null;
			if(ftc instanceof IMonitorable){
				long size=localFile.length();
				if(isRange()){
					size=getRangeSize();
				}
				p = new ProgressBar(localFile.getName(),size);
				((IMonitorable) ftc).setProgressListener(p);
			}
			FiletransferOptions.Write writer = (FiletransferOptions.Write)ftc;
			if(isRange()){
				UCC.console.verbose("Byte range: {} - {}", startByte, (getRangeSize()>0?endByte:""));
				long totalSkipped=0;
				long toSkip = startByte;
				while(totalSkipped<startByte){
					long skipped = is.skip(toSkip);
					totalSkipped+=skipped;
					toSkip-=skipped;
				}
				writer.writeAllData(is, endByte-startByte+1);

			}else{
				writer.writeAllData(is);
			}
			copyProperties(localFile, sms, remotePath);

			if(ftc instanceof IMonitorable){
				p.finish();
			}

		}finally{
			if(ftc!=null){
				try{
					// try to get some more error info
					// JSONObject props = ftc.getProperties();
					// String description = props.getString("description");
					// if("FAILED".equals(finalStatus)){
					//	msg.error("Filetransfer error: "+description, null);
					ftc.delete();
				}catch(Exception e1){}
			}
		}
		if(timing){
			long duration=System.currentTimeMillis()-startTime;
			double rate=(double)localFile.length()/(double)duration;
			UCC.console.info("Rate: {} kB/sec.", UCC.numberFormat.format(rate));
		}
	}

	/**
	 * if possible, copy the local executable flag to the remote file
	 * @param sourceFile - local file
	 * @throws Exception
	 */
	private void copyProperties(File sourceFile, StorageClient sms, String target)throws Exception{
		try{
			StringBuilder perms = new StringBuilder("r--r--");
			if(sourceFile.canExecute()){
				perms.setCharAt(2,'x');
				perms.setCharAt(5,'x');
			}
			if(sourceFile.canWrite()){
				perms.setCharAt(1,'w');
				perms.setCharAt(4,'w');
			}
			sms.chmod(target, perms.toString());
		}catch(Exception ex){
			UCC.console.error(ex, "Can't set permissions on remote file.");
		}
	}

	private void checkOK()throws FileNotFoundException{
		if(!failOnError){
			return;
		}
		File orig=new File(from);
		if(!orig.isAbsolute()){
			orig=new File(System.getProperty("user.dir"),from);
		}
		File[] files=resolveWildCards(orig);
		if(files==null){
			throw new FileNotFoundException("Local import '"+from+"' does not exist.");
		}
		for(File f: files){
			if(!f.exists())throw new FileNotFoundException("Local import '"+from+"' does not exist.");
		}
	}

	private void configure(FiletransferClient ftc, Map<String,String>params){
		if(ftc instanceof UFTPFileTransferClient){
			UFTPFileTransferClient u=(UFTPFileTransferClient)ftc;
			String secret=params.get(UFTPConstants.PARAM_SECRET);
			u.setSecret(secret);
		}
	}
	
	private void setupOffsetForResume(String remotePath, StorageClient sms) throws Exception {
		try{
			startByte = sms.stat(remotePath).size;
			endByte = Long.MAX_VALUE;
		}catch(Exception fe){
			// new file - resume is ignored
		}
	}
	
}
