package eu.unicore.ucc.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.regex.Pattern;

import eu.unicore.client.core.FileList;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.client.data.UFTPConstants;
import eu.unicore.client.data.UFTPFileTransferClient;
import eu.unicore.uas.fts.FiletransferOptions;
import eu.unicore.uas.fts.FiletransferOptions.IMonitorable;
import eu.unicore.uas.fts.FiletransferOptions.SupportsPartialRead;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.ProgressBar;

/**
 * helper that exports remote files from a UNICORE Storage 
 * to the local client machine.<br/>
 * Simple wildcards ("*" and "?") and download of 
 * directories are supported.
 * 
 * TODO this should be refactored so the single-file download logic 
 * is separated from the wildcard/directory/provided outputStream logic
 * 
 * @author schuller
 */
public class FileDownloader extends FileTransferBase {

	private boolean showProgress = true;

	private boolean forceFileOnly = false;

	private OutputStream targetStream = null;

	public FileDownloader(String from, String to, Mode mode){
		this(from, to, mode, true);
	}

	public FileDownloader(String from, String to, Mode mode, boolean failOnError){
		this.to = to;
		this.from = from;
		this.mode = mode;
		this.failOnError = failOnError;
	}

	@Override
	public void perform(StorageClient sms)throws Exception{
		assertReady(sms);
		boolean isWildcard = hasWildCards(from);
		FileListEntry remoteSource = null;
		if(isWildcard){
			performWildCardExport(sms);
		}
		else {
			//check if source is a directory
			remoteSource = sms.stat(from);
			if(remoteSource.isDirectory){
				if(forceFileOnly){
					throw new IOException("Source is a directory");
				}
				performDirectoryExport(remoteSource, new File(to), sms);
			}
			else{
				download(remoteSource,new File(to),sms);
			}
		}	
	}

	private void performDirectoryExport(FileListEntry directory, File targetDirectory, StorageClient sms)
			throws Exception {
		if(!targetDirectory.exists()|| !targetDirectory.canWrite()){
			throw new IOException("Target directory <"+to+"> does not exist or is not writable!");
		}
		if(!targetDirectory.isDirectory()){
			throw new IOException("Target <"+to+"> is not a directory!");
		}
		FileList remoteFiles = sms.ls(directory.path);
		for(FileListEntry file: remoteFiles.list(0, 1000)){
			if(file.isDirectory){
				if(!recurse) {
					UCC.console.verbose("Skipping directory {}", file.path);
					continue;
				}
				else{
					File newTargetDirectory=new File(targetDirectory,getName(file.path));
					boolean success=newTargetDirectory.mkdirs();
					if(!success)throw new IOException("Can create directory: "+newTargetDirectory.getAbsolutePath());
					performDirectoryExport(file, newTargetDirectory, sms);
					continue;
				}
			}
			download(file, new File(targetDirectory,getName(file.path)), sms);
		}
	}

	private void performWildCardExport(StorageClient sms)throws Exception{
		String dir = getDir(from);
		Pattern p = createPattern(getName(from));
		if(dir==null)dir="/";
		FileList files = sms.ls(dir);
		File targetDir = targetStream==null ? new File(to) : null;
		if(targetStream==null){
			if(!targetDir.isDirectory())throw new IOException("Target is not a directory.");
		}
		for(FileListEntry f: files.list(0, 1000)){
			if(p.matcher(new File(f.path).getName()).matches()){
				download(f, targetDir, sms);
			}
		}
	}

	private String getDir(String path){
		return new File(path).getParent();
	}

	private String getName(String path){
		return new File(path).getName();
	}

	/**
	 * download a single regular file
	 * 
	 * @param source - remote file descriptor
	 * @param localFile - local file or directory to write to
	 * @param msg
	 * @param sms
	 * @throws Exception
	 */
	private void download(FileListEntry source, File localFile, StorageClient sms)throws Exception{
		if(source==null || source.isDirectory){
			throw new IllegalStateException("Source="+source); 
		}
		OutputStream os = targetStream;
		FiletransferClient ftc=null;
		boolean resume = false;
		boolean append = Mode.APPEND.equals(mode) || resume;
		try{
			String path = source.path;
			if(os==null){
				if(localFile.isDirectory()){
					localFile=new File(localFile,getName(path));
				}
				if(mode.equals(Mode.NO_OVERWRITE) && localFile.exists()){
					UCC.console.verbose("File exists and creation mode was set to 'nooverwrite'.");
					return; 
				}
				UCC.console.verbose("Downloading remote file '{}/files/{}' -> {}", 
						sms.getEndpoint().getUrl(), path, localFile.getAbsolutePath());
				if(resume){
					setupOffsetForResume(localFile);
				}
				os = new FileOutputStream(localFile.getAbsolutePath(), append);
			}
			chosenProtocol = determineProtocol(preferredProtocol, sms);
			Map<String,String>extraParameters = getExtraParameters(chosenProtocol);
			ftc = sms.createExport(path, chosenProtocol, extraParameters);
			configure(ftc, extraParameters);
			UCC.console.verbose("File transfer URL: {}", ftc.getEndpoint().getUrl());
			ProgressBar p=null;
			if(ftc instanceof IMonitorable  && showProgress){
				long size = source.size;
				if(isRange()){
					size=getRangeSize();
				}
				p = new ProgressBar(localFile.getName(),size);
				((IMonitorable) ftc).setProgressListener(p);
			}
			long startTime=System.currentTimeMillis();
			if(isRange()){
				if(!(ftc instanceof SupportsPartialRead)){
					throw new Exception("Byte range is defined but protocol does not allow " +
							"partial read! Please choose a different protocol!");
				}
				UCC.console.verbose("Byte range: {} - {}", startByte, (getRangeSize()>0?endByte:""));
				SupportsPartialRead pReader=(SupportsPartialRead)ftc;
				long length = endByte-startByte+1;
				if(Long.MAX_VALUE==endByte){
					length = source.size - startByte;
				}
				if(length>0)pReader.readPartial(startByte, length, os);
			}
			else{
				if(!(ftc instanceof FiletransferOptions.Read)){
					throw new Exception("Protocol does not support read operation! Please choose a different protocol!");
				}
				((FiletransferOptions.Read)ftc).readAllData(os);
			}
			if(p!=null){
				p.finish();
			}
			if(timing){
				long duration=System.currentTimeMillis()-startTime;
				double rate=(double)localFile.length()/(double)duration;
				UCC.console.info("Rate: {} kB/sec.", UCC.numberFormat.format(rate));
			}
			if(targetStream==null)copyProperties(source, localFile);
		}
		finally{
			if(targetStream==null){
					try {
						os.close();
					}catch(Exception ex) {}
			}
			if(ftc!=null){
				try{
					// try to get some more error info
					// JSONObject props = ftc.getProperties();
					// String description = props.optString("description");
					// String finalStatus = props.optString("status");
					// if("FAILED".equals(finalStatus)){
					//	 msg.error("Filetransfer error: "+description, null);
					// }
					ftc.delete();
				}catch(Exception e1){}
			}
		}
	}

	/**
	 * if possible, copy the remote executable flag to the local file
	 * @param targetFile - local file
	 * @throws Exception
	 */
	private void copyProperties(FileListEntry source, File localFile)throws Exception{
		try{
			localFile.setExecutable(source.permissions.contains("x"));
		}
		catch(Exception ex){
			UCC.console.error(ex, "Can't set 'executable' flag for {}", localFile.getName());
		}
	}

	private void configure(FiletransferClient ftc, Map<String,String>params){
		if(ftc instanceof UFTPFileTransferClient){
			UFTPFileTransferClient u=(UFTPFileTransferClient)ftc;
			String secret=params.get(UFTPConstants.PARAM_SECRET);
			u.setSecret(secret);
		}
	}

	public void setShowProgress(boolean showProgress) {
		this.showProgress = showProgress;
	}

	public void setForceFileOnly(boolean forceFileOnly) {
		this.forceFileOnly = forceFileOnly;
	}

	public void setTargetStream(OutputStream targetStream) {
		this.targetStream = targetStream;
	}

	private void setupOffsetForResume(File localFile) throws Exception {
		if(localFile.exists()){
			startByte = localFile.length();
			endByte = Long.MAX_VALUE;
		}
	}
}
