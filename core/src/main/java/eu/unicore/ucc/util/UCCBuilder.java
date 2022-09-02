package eu.unicore.ucc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.Builder;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.json.Requirement;
import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.uas.util.UnitParser;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.actions.data.Resolve;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.helpers.DefaultMessageWriter;
import eu.unicore.ucc.io.FileDownloader;
import eu.unicore.ucc.io.FileTransferBase.Mode;
import eu.unicore.ucc.io.FileUploader;
import eu.unicore.ucc.io.Location;

/**
 * in addition to the basic Builder class, this 
 * supports local imports / exports 
 * @author schuller
 */
public class UCCBuilder extends Builder {

	private File baseDirectory;
	private final List<FileUploader> imports;
	private final List<FileDownloader> exports;
	
	private MessageWriter msg = new DefaultMessageWriter();
	private final UCCConfigurationProvider configurationProvider;
	private final IRegistryClient registry;
	private boolean checkLocalFiles=true;
	
	/**
	 * reads a JSON string from the supplied File
	 * and creates the builder from it
	 * 
	 * @param jsonFile
	 * @throws Exception
	 */
	public UCCBuilder(File jsonFile, IRegistryClient registry, UCCConfigurationProvider configurationProvider)throws Exception{
		this(FileUtils.readFileToString(jsonFile, "UTF-8"), registry, configurationProvider);
		this.baseDirectory = jsonFile.getParentFile()!=null?jsonFile.getParentFile():new File(".");
	}

	/**
	 * Creates the builder from the supplied JSON string
	 * 
	 * @param jsonString
	 * @throws Exception
	 */
	public UCCBuilder(String jsonString, IRegistryClient registry, UCCConfigurationProvider configurationProvider) {
		super(jsonString);
		this.baseDirectory = new File(".");
		exports = new ArrayList<>();
		imports = new ArrayList<>();
		this.configurationProvider = configurationProvider;
		this.registry = registry;
	}


	/**
	 * Creates an empty builder. All content has to be set via the API
	 * @throws Exception
	 */
	public UCCBuilder(IRegistryClient registry, UCCConfigurationProvider configurationProvider) {
		this("{}", registry, configurationProvider);
	}

	public void setMessageWriter(MessageWriter msg){
		this.msg=msg;
	}

	public MessageWriter getMessageWriter(){
		return msg;
	}
	
	@Override
	public JSONObject getJSON() {
		build();
		if(imports.size()>0) {
			try{
				json.put("haveClientStageIn", true);
			}catch(JSONException e) {}
		}
		return super.getJSON();
	}

	protected void build(){
		if(initialised)return;
		super.build();
		try{
			JSONArray nonLocalImports = createLocalImports(json.optJSONArray("Imports"));
			json.put("Imports", nonLocalImports);
			JSONArray nonLocalExports = createLocalExports(json.optJSONArray("Exports"));
			json.put("Exports", nonLocalExports);
			resolveLocations(json.optJSONArray("Imports"), "From");
			resolveLocations(json.optJSONArray("Exports"), "To");
		}catch(Exception ex){
			msg.error("Error building imports/exports for job", ex);
			throw new RuntimeException(ex);
		}
	}
	
	private void resolveLocations(JSONArray stages, String key) {
		if(stages==null || stages.length()==0)return;
		
		for(int i=0; i<stages.length(); i++) {
			JSONObject stage = null;
			try {
				stage = stages.getJSONObject(i);
				String uri = stage.getString(key);
				Location l = Resolve.resolve(uri, registry, configurationProvider, msg);
				stage.put(key, l.getEndpointURL());
			}catch(JSONException je) {
				throw new IllegalArgumentException("Malformed staging directive: "
						+(stage!=null? stage.toString() : "in "+stages.toString()));
			}
		}
	}
	
	private JSONArray createLocalImports(JSONArray j)throws IllegalArgumentException, FileNotFoundException{
		JSONArray otherImports = new JSONArray();
		if(j!=null){
			for (int i = 0; i < j.length(); i++) {
				String source,target;
				Mode mode=Mode.NORMAL;
				boolean failOnError=true;
				try{
					JSONObject jObj=j.getJSONObject(i);
					source=JSONUtil.getString(jObj,"From");
					target=JSONUtil.getString(jObj,"To");
					String creation=JSONUtil.getString(jObj,"Mode","NORMAL");
					failOnError=Boolean.parseBoolean(JSONUtil.getString(jObj,"FailOnError","true"));
					mode=Mode.valueOf(creation);
					if(source==null || target==null){
						throw new IllegalArgumentException("File import specification invalid. Syntax: \"From: <source>, To: <uspacefile>, Mode: overwrite|append|nooverwrite\"");
					}
					Location l = createLocation(source);
					if(l.isLocal()){
						failOnError=failOnError & checkLocalFiles;
						imports.add(new FileUploader(baseDirectory, source,target,mode,failOnError));
						msg.verbose("Local file import: "+source+" -> "+target);
					}
					else {
						otherImports.put(jObj);
					}
				}catch(JSONException je) {
					throw new RuntimeException(je);
				}
				
			}
		}
		return otherImports;
	}

	
	@Override
	protected Location createLocation(String descriptor) {
		return Resolve.resolve(descriptor, registry, configurationProvider, msg);
	}

	private JSONArray createLocalExports(JSONArray j)throws IllegalArgumentException {
		JSONArray otherExports = new JSONArray();
		if(j!=null){
			for (int i = 0; i < j.length(); i++) {
				String source,target;
				Mode mode=Mode.NORMAL;
				boolean failOnError=true;
				try{
					JSONObject jObj=j.getJSONObject(i);
					source=JSONUtil.getString(jObj,"From");
					if(source==null)source=JSONUtil.getString(jObj,"File");//backwards compatibility
					target=jObj.getString("To");
					String creation=JSONUtil.getString(jObj,"Mode","NORMAL");
					mode=Mode.valueOf(creation);
					failOnError=Boolean.parseBoolean(JSONUtil.getString(jObj,"FailOnError","true"));

					if(source==null || target==null){
						throw new IllegalArgumentException("Local export specification invalid. Syntax: \"From: <uspacefile | remotefile>, To: <target>, Mode: overwrite|append|nooverwrite\"");
					}
					Location l = createLocation(target);
					if(l.isLocal()){
						exports.add(new FileDownloader(source, target, mode, failOnError));
						msg.verbose("File export to client: "+source+" -> "+target);
					}
					else {
						otherExports.put(jObj);
					}
				}catch(JSONException e){
					throw new RuntimeException(e);
				}
			}
		}
		return otherExports;
	}

	public File getBaseDirectory() {
		return baseDirectory;
	}

	public List<FileDownloader> getExports() {
		build();
		return exports;
	}

	public List<FileUploader> getImports() {
		build();
		return imports;
	}
	
	public void setCheckLocalFiles(boolean checkLocalFiles){
		this.checkLocalFiles=checkLocalFiles;
	}

	public String getApplicationName(){
		return JSONUtil.getString(json, "ApplicationName");
	}

	public String getApplicationVersion(){
		return JSONUtil.getString(json, "ApplicationVersion");
	}
	
	public String getSite(){
		return JSONUtil.getString(json, "Site", null);
	}
	
	/**
	 * get the preferred file transfer protocols (if defined)
	 */
	public String getPreferredProtocol(){
		return preferredProtocol;
	}

	public void setPreferredProtocol(String protocol){
		JSONUtil.putQuietly(json, "Preferred protocol", protocol.toString());
		this.preferredProtocol = protocol;
	}

	public Set<Requirement>getRequirements(){
		build();
		return requirements;
	}
	
	public int getLifetime(){
		String lifetime =JSONUtil.getString(json,"Lifetime");
		if(lifetime!=null && lifetime.length()>0){
			return (int)UnitParser.getTimeParser(0).getDoubleValue(lifetime);
		}
		else return -1;
	}
	
	public void addTags(String[]tags) {
		JSONArray existingTags = json.optJSONArray("Tags");
		if(existingTags==null)existingTags = json.optJSONArray("tags");
		if(existingTags==null) {
			existingTags = new JSONArray();
			JSONUtil.putQuietly(json, "Tags", existingTags);
		}
		try {
			List<String> existing = JSONUtil.toList(existingTags);
			for(String t: tags) {
				if(!existing.contains(t)) {
					existing.add(t);
					existingTags.put(t);
				}
			}
		}catch(JSONException je) {}
	}

	public static String getJobSample(){
		StringBuilder s=new StringBuilder();
		writeLine("{",s);
		writeLine("  ApplicationName: \"Date\", ApplicationVersion: \"1.0\",",s);
		writeLine("  Executable: \"/bin/date\",",s);
		writeLine("  Arguments: [\"-l\", \"-v\", ],",s);
		writeLine("  Parameters: { VERBOSE: true, INPUTFILE: foo.txt},",s);
		writeLine("  Environment: [\"VERBOSE=true\", \"TIMING=false\", ],",s);
		writeLine("  Job type: \"normal | interactive\",",s);
		writeLine("", s);
		writeLine("  Imports: [",s);
		writeLine("    { From: \"localFile | remoteFile\", To: \"jobdirFile\", Mode: \"overwrite | append | nooverwrite\" , FailOnError: \"true | false\" },",s);
		writeLine("  ],",s);
		writeLine("  Exports: [",s);
		writeLine("    { From: \"jobdirFile\", To: \"localFile | remoteFile\", Mode: \"overwrite | append | nooverwrite\" }, ",s);
		writeLine("  ],",s);
		writeLine("  Umask: \"0022\",",s);
		writeLine("  Name: \"my test job\",",s);
		writeLine("  User email: foo@bar.org,",s);
		writeLine("  Notification: \"https://someservice.org\",",s);
		writeLine("  Tags: [\"testing\", \"demo\"] ,",s);
		writeLine("  Not before: \"2021-11-11T12:00:00+200\",",s);
		writeLine("  Site: \"DEMO-SITE\",",s);
		writeLine("  Project: some_project,",s);
		writeLine("  Resources: {",s);
		writeLine("    Queue: batch,",s);
		writeLine("    CPUs: 4,",s);
		writeLine("    Nodes: 2,",s);
		writeLine("    CPUsPerNode: 2,",s);
		writeLine("    Memory: 2048M,",s);
		writeLine("    Runtime: 4h,",s);
		writeLine("    Reservation: 1234,",s);
		writeLine("  },",s);
		writeLine("  Preferred protocols: \"UFTP BFT\",",s);
		writeLine("  Output: \"localdir\",",s);
		writeLine("}",s);
		return s.toString();
	}

	static final String lineSep=System.getProperty("line.separator");

	static void writeLine(String line,StringBuilder sb){
		sb.append(line);
		sb.append(lineSep);
	}

	
}
