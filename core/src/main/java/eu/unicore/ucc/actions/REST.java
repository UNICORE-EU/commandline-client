package eu.unicore.ucc.actions;


import java.io.File;
import java.util.Formatter;

import org.apache.commons.cli.Option;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.ucc.IServiceInfoProvider;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * low-level REST API interactions
 * 
 * @author schuller
 */
public class REST extends ActionBase implements IServiceInfoProvider {

	public static final String OPT_ACCEPT_LONG = "accept";
	public static final String OPT_ACCEPT = "A";
	public static final String OPT_CONTENT_LONG = "content-type";
	public static final String OPT_CONTENT = "C";
	public static final String OPT_INCLUDE_LONG = "include";
	public static final String OPT_INCLUDE = "i";


	private String accept;
	private String contentType;
	private boolean includeHeaders;

	@Override
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(Option.builder(OPT_ACCEPT)
				.longOpt(OPT_ACCEPT_LONG)
				.desc("Value for the 'Accept' HTTP header (default: 'application/json')")
				.required(false)
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_CONTENT)
				.longOpt(OPT_CONTENT_LONG)
				.desc("Value for the 'Content-Type' HTTP header (default: 'application/json')")
				.required(false)
				.hasArg()
				.build());
		getOptions().addOption(Option.builder(OPT_INCLUDE)
				.longOpt(OPT_INCLUDE_LONG)
				.desc("Include the response HTTP headers in the output")
				.required(false)
				.build());
	}

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

	@Override
	public String getCommandGroup(){
		return CMD_GRP_UTILITY;
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
			accept = getCommandLine().getOptionValue(OPT_ACCEPT, "application/json");
			contentType = getCommandLine().getOptionValue(OPT_CONTENT, "application/json");
			includeHeaders = getCommandLine().hasOption(OPT_INCLUDE);
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
		verbose("Accessing endpoint <"+url+">");
		BaseClient bc = makeClient(url);
		ContentType ct = ContentType.create(contentType);

		if("get".startsWith(cmd.toLowerCase())){
			try(ClassicHttpResponse response = bc.get(ContentType.create(accept))){
				handleResponse(response, bc);
			}
		}
		else if("delete".startsWith(cmd.toLowerCase())){
			bc.delete();
		}
		else if("post".startsWith(cmd.toLowerCase())){
			try(ClassicHttpResponse response = bc.post(content)){
				handleResponse(response, bc);
			}
		}
		else if("put".startsWith(cmd.toLowerCase())){
			try(ClassicHttpResponse response = bc.put(IOUtils.toInputStream(content.toString(), "UTF-8"), ct)){
				handleResponse(response, bc);
			}
		}
		else {
			throw new IllegalArgumentException("Command <"+cmd+"> not implemented / not understood!");
		}
	}

	protected void handleResponse(ClassicHttpResponse res, BaseClient bc) throws Exception {
		bc.checkError(res);
		try {
			message(new StatusLine(res).toString());
			Header l = res.getFirstHeader("Location");
			if(l!=null) {
				message(l.getValue());
			}
			if (includeHeaders) for(Header h: res.getHeaders()) {
				message(h.getName()+": "+h.getValue());
			}
			if("application/json".equalsIgnoreCase(accept)) {
				message(bc.asJSON(res).toString(2));
			}
			else {
				message(EntityUtils.toString(res.getEntity(), "UTF-8"));
			}
		}catch(Exception ex) {}
	}
	
	protected BaseClient makeClient(String url) throws Exception {
		return new BaseClient(url,
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
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
		try(Formatter f = new Formatter(sb)){
			f.format("  * authenticated as: '%s' role='%s' uid='%s'%s",
					client.getString("dn"), role, uid, cr);
		}
	}
	
	private void serverDetails(StringBuilder sb, JSONObject server) throws JSONException {
		String cr = System.getProperty("line.separator");
		String dn = null;
		try{
			dn = server.getJSONObject("credential").getString("dn");
		}catch(JSONException ex) {
			dn = server.getString("dn");
		}
		try(Formatter f = new Formatter(sb)){
			f.format("* server v%s %s %s", server.optString("version", "n/a"), dn, cr);
		}
	}

}
