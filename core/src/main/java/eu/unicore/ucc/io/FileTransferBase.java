package eu.unicore.ucc.io;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import org.json.JSONObject;

import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.uas.FiletransferParameterProvider;
import eu.unicore.uas.util.PropertyHelper;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.util.JSONUtil;

public abstract class FileTransferBase {

	protected Properties extraParameterSource;

	protected final Map<String,String>extraParameters = new HashMap<>();

	protected boolean timing=false;

	protected boolean recurse=false;

	protected String from;

	protected String to;

	//index of first byte to download
	protected Long startByte;
	
	//index of last byte to download
	protected Long endByte;

	public static enum Mode {
			NORMAL, APPEND, RESUME, NO_OVERWRITE
	};

	protected Mode mode;

	/**
	 * whether the job processing should fail if an error occurs
	 */
	protected boolean failOnError;

	protected String preferredProtocol;

	public abstract void perform(StorageClient sms)throws Exception;
	
	protected Map<String,String> getExtraParameters(String protocol){
		Map<String,String> res = new HashMap<>();
		if(extraParameterSource!=null){
			String p=String.valueOf(protocol);
			PropertyHelper ph=new PropertyHelper(extraParameterSource, new String[]{p,p.toLowerCase()});
			res.putAll(ph.getFilteredMap());
		}
		ServiceLoader<FiletransferParameterProvider> ppLoader = ServiceLoader.load(FiletransferParameterProvider.class);
		Iterator<FiletransferParameterProvider> ppIter = ppLoader.iterator();
		while(ppIter.hasNext()) {
			FiletransferParameterProvider pp = ppIter.next();
			pp.provideParameters(res, protocol);
		}
		res.putAll(extraParameters);
		if(res.size()>0){
			UCC.console.debug("Have {} extra parameters for protocol {}", extraParameters.size(), protocol);
		}
		return res;
	}
	
	protected String resolveFromBaseDir(String child, File baseDirectory) {
		File childF = new File(child);
		if(childF.isAbsolute()) {
			return child;
		}
		return new File(baseDirectory, child).getAbsolutePath();
	}
	
	public String getTo() {
		return to;
	}

	public String getFrom() {
		return from;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public boolean isFailOnError() {
		return failOnError;
	}

	public boolean isTiming() {
		return timing;
	}

	public void setTiming(boolean timing) {
		this.timing = timing;
	}

	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	public String getPreferredProtocol() {
		return preferredProtocol;
	}

	public void setPreferredProtocol(String preferredProtocol) {
		this.preferredProtocol = preferredProtocol;
	}

	public void setExtraParameterSource(Properties properties){
		this.extraParameterSource=properties;
	}

	public void setExtraParameters(Map<String,String>params){
		this.extraParameters.putAll(params);
	}

	public void setRecurse(boolean recurse) {
		this.recurse = recurse;
	}
	/**
	 * check if the given path denotes a valid remote directory
	 * @param remotePath - the path
	 * @param sms - the storage
	 * @return <code>true</code> if the remote directory exists and is a directory
	 */
	protected boolean isValidDirectory(String remotePath, StorageClient sms){
		boolean result = false;
		if(! ("/".equals(remotePath) || ".".equals(remotePath)) ){
			try{
				FileListEntry gft = sms.stat(remotePath);
				result = gft.isDirectory;
			}catch(Exception ex){}
		}
		else result=true;
		
		return result;
	}
	
	public File[] resolveWildCards(File original){
		final String name=original.getName();
		if(!hasWildCards(original))return new File[]{original};
		File parent=original.getParentFile();
		if(parent==null)parent=new File(".");
		Pattern p=createPattern(name);
		return parent.listFiles( (f, n) -> p.matcher(n).matches());
	}

	protected boolean hasWildCards(File file){
		return hasWildCards(file.getName());
	}

	public boolean hasWildCards(String name){
		return name.contains("*") || name.contains("?");
	}

	protected Pattern createPattern(String nameWithWildcards){
		String regex=nameWithWildcards.replace("?",".").replace("*", ".*");
		return Pattern.compile(regex);
	}
	
	protected String chosenProtocol=null;
	
	public String getChosenProtocol(){
		return chosenProtocol;
	}

	public Long getStartByte() {
		return startByte;
	}

	public void setStartByte(Long startByte) {
		this.startByte = startByte;
	}

	public Long getEndByte() {
		return endByte;
	}

	public void setEndByte(Long endByte) {
		this.endByte = endByte;
	}
	
	/**
	 * checks if a byte range is defined
	 * @return <code>true</code> iff both startByte and endByte are defined
	 */
	protected boolean isRange(){
		return startByte!=null && endByte!=null;
	}
	
	/**
	 * get the number of bytes in the byte range, or "-1" if the range is open-ended
	 */
	protected long getRangeSize(){
		if(Long.MAX_VALUE==endByte)return -1;
		return endByte-startByte;
	}
	
	protected String determineProtocol(String preferred, StorageClient sms) {
		if(preferred!=null)try{
			List<String> supported = JSONUtil.asList(sms.getProperties().getJSONArray("protocols"));
			for(String s: supported) {
				if(s.equalsIgnoreCase(preferred))return s;
			}
		}catch(Exception ex) {}
		return "BFT";
	}

	protected void assertReady(StorageClient sms) throws Exception {
		int i=0;
		while(i < 60) {
			JSONObject props = sms.getProperties("resourceStatus");
			if("READY".equals(props.optString("resourceStatus","n/a"))){
				return;
			}
			else {
				i++;
				Thread.sleep(1000);
			}
		}
		throw new Exception("Timeout waiting for Storage <"+
				sms.getEndpoint().getUrl()+"> to become ready");
	}

}
