package eu.unicore.ucc.actions;


import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.ucc.IServiceInfoProvider;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * low-level REST API interactions
 * 
 * @author schuller
 */
public class REST extends ActionBase implements IServiceInfoProvider {

	@Override
	public String getName(){
		return "rest";
	}

	@Override
	public String getArgumentList(){
		return "GET|PUT|POST|DELETE [JSON-string | @file] URL [URLS...]";
	}

	@Override
	public String getSynopsis(){
		return "Low-level REST API operations";
	}

	@Override
	public String getDescription(){
		return "perform a low-level REST API operation";
	}

	protected boolean requireRegistry(){
		return false;
	}
	
	protected boolean skipConnectingToRegistry() {
		return true;
	}
	
	@Override
	public void process(){
		super.process();
		try{
			int length=getCommandLine().getArgs().length;
			if(length<2){
				throw new IllegalArgumentException("You must provide at least a command (GET, PUT, ...) as argument.");
			}
			String cmd=getCommandLine().getArgs()[1];
			if(length<3){
				throw new IllegalArgumentException("You must provide at least a URL as argument to this command.");
			}
			int startIndex = 2;
			JSONObject content = new JSONObject(); 
			if(length>3){
				String ref = getCommandLine().getArgs()[2];
				if(ref.startsWith("@")){
					File f = new File(ref.substring(1));
					content = new JSONObject(FileUtils.readFileToString(f, "UTF-8"));
					startIndex++;
				}
				else if(!ref.toLowerCase().startsWith("http")) {
					try{
						content = new JSONObject(ref);
						startIndex++;
					}catch(JSONException je) {}
				}
			}
			
			for(int i = startIndex; i<length; i++) {
				String url=getCommandLine().getArgs()[i];
				doProcess(cmd, url, content);
			}
			
		}catch(Exception e){
			error("Can't perform REST operation", e);
			endProcessing(ERROR);
		}
	}
	
	protected void doProcess(String cmd, String url, JSONObject content) throws Exception {
		verbose("Connecting to <"+url+">");
		BaseClient bc = makeClient(url);
	
		if("get".startsWith(cmd.toLowerCase())){
			message(bc.getJSON().toString(2));
		}
		else if("delete".startsWith(cmd.toLowerCase())){
			bc.delete();
		}
		else if("post".startsWith(cmd.toLowerCase())){
			handleResponse(bc.post(content), bc);
		}
		else if("put".startsWith(cmd.toLowerCase())){
			handleResponse(bc.put(content), bc);
		}
		else {
			throw new IllegalArgumentException("Command <"+cmd+"> not (yet) implemented / not understood!");
		}
	}
	
	protected void handleResponse(HttpResponse res, BaseClient bc) throws Exception {
		bc.checkError(res);
		try {
			message(res.getStatusLine().toString());
			Header l = res.getFirstHeader("Location");
			if(l!=null) {
				message(l.getValue());
			}
			for(Header h: res.getAllHeaders()) {
				verbose(h.getName()+": "+h.getValue());
			}
			message(bc.asJSON(res).toString(2));
		}catch(Exception ex) {}
	}
	
	protected BaseClient makeClient(String url) throws Exception {
		IClientConfiguration securityProperties = configurationProvider.getClientConfiguration(url);
		IAuthCallback auth = configurationProvider.getRESTAuthN();
		return new BaseClient(url, securityProperties, auth);
	}
	
	@Override
	public String getType() {
		return "CoreServices";
	}

	@Override
	public String getServiceName() {
		return "RESTful Core";
	}

	@Override
	public String getServiceDetails(Endpoint epr, UCCConfigurationProvider configurationProvider){
		String url = epr.getUrl();
		StringBuilder sb = new StringBuilder();
		try{
			IClientConfiguration securityProperties = configurationProvider.getClientConfiguration(url);
			IAuthCallback auth = configurationProvider.getRESTAuthN();
			BaseClient bc = new BaseClient(url, securityProperties, auth);
			JSONObject props = bc.getJSON();
			serverDetails(sb, props.getJSONObject("server"));
			clientDetails(sb, props.getJSONObject("client"));
		}catch(Exception ex) {
			error("Error accessing REST service at <"+url+">", ex);
		}
		return sb.toString();
	}
	
	private void clientDetails(StringBuilder sb, JSONObject client) throws JSONException {
		String cr = System.getProperty("line.separator");
		String role = client.getJSONObject("role").getString("selected");
		String uid = client.getJSONObject("xlogin").optString("UID","n/a");
		sb.append("  * authenticated as: '").append(client.getString("dn")).append("' role='").append(role).append("'");
		sb.append(" uid='").append(uid).append("'");
		sb.append(cr);
	}
	
	private void serverDetails(StringBuilder sb, JSONObject server) throws JSONException {
		String cr = System.getProperty("line.separator");
		sb.append("* server v").append(server.optString("version", "n/a"));
		
		String dn = null;
		try{
			dn = server.getJSONObject("credential").getString("dn");
		}catch(JSONException ex) {
			dn = server.getString("dn");
		}
		sb.append(" ").append(dn).append(cr);
	}

}
