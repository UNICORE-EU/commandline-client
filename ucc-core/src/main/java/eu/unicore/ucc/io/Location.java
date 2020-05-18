package eu.unicore.ucc.io;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fzj.unicore.uas.util.MessageWriter;

/**
 * A local or remote file location. Can be specified in in the following ways
 * <ul>
 * 
 *   <li>either by a direct UNICORE (RESTful) URI
 *   of the form <pre>PROTOCOL:ADDRESS/files/file</pre>
 * 
 *   <li>UNICORE URI without protocol specification, 
 *   <pre>ADDRESS/files/file</pre>
 * 
 *   <li>"Raw" locations (such as "ftp://..." or "http://..." URLs) are also supported
 * 
 * </ul>
 * 
 * If no protocol is given in the URL, the default protocol (BFT) is used. <br/>
 * 
 * @author schuller
 */
public class Location implements de.fzj.unicore.uas.json.Location {

	// the raw location as passed to the constructor
	protected final String originalDescriptor;
	
	protected boolean isLocal=false;
	protected boolean isRaw=false;
	
	protected String smsEpr;
	protected String protocol;
	protected String name;
	protected final String defaultProtocol;
	
	/**
	 * Constructs a new Location
	 * 
	 * @param desc - the string describing the location
	 * @param registry - the {@link RegistryClient} pointing to the registry
	 * @param sec - security settings
	 * @param msg -  a {@link MessageWriter} instance
	 * @param doResolve - if <code>false</code>, no resolution of unicore:// URLs will be performed
	 * @param defaultProtocol -  the default protocol
	 */
	public Location(String desc, String defaultProtocol){
		this.originalDescriptor = desc;
		this.defaultProtocol = defaultProtocol;
		if(parseDesc(desc)==false)throw new IllegalArgumentException("<"+desc+"> is not a valid location.");
	}

	public Location(String desc){
		this(desc, "BFT");
	}
	
	//pattern describing a UNICORE REST file URL: <PROTOCOL>:https://<url>/rest/core/storages/<storage_name>/<files>/path
	final protected static String u8URLRE= "(([[\\w-]]+):)?([\\w-])+://.*/rest/core/storages/(.)*(/files/)?(.)*";
	final public static Pattern pattern = Pattern.compile(u8URLRE); 

	public static boolean isUNICORE_URL(String url){		
		Matcher m=pattern.matcher(url);
		return m.find();
	}

	final protected static String rawURLRE= "([\\w-])+:(.)*";
	final public static Pattern rawPattern = Pattern.compile(rawURLRE); 

	public static boolean isRawURL(String url){	
		boolean isU=isUNICORE_URL(url);
		Matcher m=rawPattern.matcher(url);
		return !isU && m.find();
	}
	
	/**
	 * parses the supplied location. This can be either a 
	 * remote or local file
	 * 
	 * @param desc
	 * @return <code>true</code> if the location could be resolved
	 */
	protected boolean parseDesc(String desc){

		//check if it is an SMS UNICORE URI
		if(isUNICORE_URL(desc)){
			if(!desc.contains("/files")) {
				// user only gave a storage endpoint
				desc = desc+"/files/";
			}
			String[] s1=desc.split("/files/",2);
			try{
				name=s1[1];
				String s2[]=s1[0].split(":",2);
				if(s2[1].toLowerCase().startsWith("http")){
					protocol=s2[0];
					smsEpr=s2[1];
				}
				else{
					//no protocol, use default
					protocol=defaultProtocol;
					smsEpr=s1[0];
				}
				return true;
			}catch(Exception e){
				return false;
			}
		}
		//then check if it is a "raw" URL, i.e. does *not* have the form "PROTOTOL:scheme://..."
		if(isRawURL(desc)){
			smsEpr=desc;
			name=null;
			isRaw=true;
			return true;
		}

		//finally assume it is just a file path
		name=desc;
		isLocal=true;
		return true;

	}

	@Override
	public boolean isLocal() {
		return isLocal;
	}

	@Override
	public boolean isUnicoreURL() {
		return !isRaw && !isLocal;
	}
	
	@Override
	public boolean isRaw() {
		return isRaw;
	}

	public String getName() {
		return name;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol=protocol;
	}

	public String getSmsEpr() {
		return smsEpr;
	}

	public String getUnicoreURI() {
		return getUnicoreURI(protocol);
	}
	
	/**
	 * get this location as a UNICORE RESTful URI using the given protocol, i.e.
	 * <code>protocol://SMS-URL/files/path</code> <br/>
	 * for example
	 * <code>BFT://https://localhost:8080/DEMO-SITE/rest/core/storages/WORK/files/test</code> <br/>
	 * 
	 * @param protocol - the protocol to use. If null, the default protocol (usually BFT) will be used
	 */
	public String getUnicoreURI(String protocol) {
		if(isLocal() || smsEpr==null)return null;
		String p = protocol!=null?protocol:defaultProtocol;
		return p+":"+smsEpr+"/files/"+(name!=null?name:"");
	}
	
	public String toString(){
		return getClass().getName()+"["+originalDescriptor+"]";
	}
	
	@Override
	public String getEndpointURL() {
		return originalDescriptor;
	}
}
