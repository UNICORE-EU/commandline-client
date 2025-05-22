package eu.unicore.ucc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.uas.json.Builder;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.json.Requirement;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.actions.data.Resolve;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.helpers.ConsoleLogger;
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
	
	private ConsoleLogger msg = new ConsoleLogger();
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

	public void setMessageWriter(ConsoleLogger msg){
		this.msg=msg;
	}

	public ConsoleLogger getMessageWriter(){
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
			msg.error(ex, "Error building imports/exports for job");
			throw new RuntimeException(ex);
		}
	}
	
	private void resolveLocations(JSONArray stages, String key) {
		if(stages==null || stages.length()==0)return;
		
		for(int i=0; i<stages.length(); i++) {
			JSONObject stage = null;
			try {
				stage = stages.getJSONObject(i);
				String uri = stage.optString(key, null);
				if(uri==null)continue;
				Location l = Resolve.resolve(uri, registry, configurationProvider);
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
					// source can be null if it is an inline import
					if(source==null && target!=null && JSONUtil.getString(jObj, "Data")==null) {
						throw new IllegalArgumentException("File import specification invalid: need one of 'From' or 'Data'.");
					}
					if(target==null){
						throw new IllegalArgumentException("File import specification invalid: 'To' is missing.");
					}
					Location l = null;
					if(source!=null) {
						l = createLocation(source);
					}
					if(l!=null && l.isLocal()){
						failOnError=failOnError & checkLocalFiles;
						imports.add(new FileUploader(baseDirectory, source, target, mode, failOnError));
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

	protected Location createLocation(String descriptor) {
		return Resolve.resolve(descriptor, registry, configurationProvider);
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
	
	static final String lineSep=System.getProperty("line.separator");

	static void writeLine(String line,StringBuilder sb){
		sb.append(line);
		sb.append(lineSep);
	}

	public void writeTo(Writer writer)throws IOException{
		writer.write(getJSON().toString(2));
	}
	
}
