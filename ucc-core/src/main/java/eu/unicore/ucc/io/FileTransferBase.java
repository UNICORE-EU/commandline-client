package eu.unicore.ucc.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.uas.util.PropertyHelper;
import de.fzj.unicore.ucc.util.JSONUtil;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;

public abstract class FileTransferBase {

	protected Properties extraParameterSource;

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

	protected List<String> preferredProtocols=new ArrayList<>();

	public abstract void perform(StorageClient sms, MessageWriter msg)throws Exception;
	
	protected Map<String,String>makeExtraParameters(String protocol, MessageWriter msg){
		Map<String, String> res;
		if(extraParameterSource==null){
			res=new HashMap<String, String>();
		}
		else{
			String p=String.valueOf(protocol);
			PropertyHelper ph=new PropertyHelper(extraParameterSource, new String[]{p,p.toLowerCase()});
			res= ph.getFilteredMap();
		}
		if(res.size()>0){
			msg.verbose("Have "+res.size()+" extra parameters for protocol "+protocol);
		}
		return res;
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

	public List<String> getPreferredProtocols() {
		return preferredProtocols;
	}

	public void setPreferredProtocols(List<String> preferredProtocols) {
		this.preferredProtocols = preferredProtocols;
	}

	public void setExtraParameterSource(Properties properties){
		this.extraParameterSource=properties;
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
		FilenameFilter filter=new FilenameFilter(){
			Pattern p=createPattern(name);
			public boolean accept(File file, String name){
				return p.matcher(name).matches();
			}
		};
		return parent.listFiles(filter);
	}

	protected boolean hasWildCards(File file){
		return hasWildCards(file.getName());
	}

	public boolean hasWildCards(String name){
		return name.contains("*") || name.contains("?");
	}

	private Pattern createPattern(String nameWithWildcards){
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
	
	protected String determineProtocol(List<String>preferred, StorageClient sms) {
		try{
			List<String> supported = JSONUtil.asList(sms.getProperties().getJSONArray("protocols"));
			for(String prefer: preferred) {
				for(String s: supported) {
					if(prefer.equalsIgnoreCase(s))return s;
				}
			}
		}catch(Exception ex) {}
		return "BFT";
	}
	
}
