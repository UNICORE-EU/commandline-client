package eu.unicore.ucc.actions;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.commons.io.FileUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONObject;

import eu.unicore.client.core.CoreClient;
import eu.unicore.client.lookup.CoreEndpointLister;
import eu.unicore.client.lookup.SiteNameFilter;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.restclient.utils.UnitParser;
import eu.unicore.ucc.UCC;

/**
 * Call a /rest/token endpoint to issue a JWT token 
 *
 * @author schuller
 */
public class IssueToken extends ActionBase {

	// token lifetime (in seconds)
	private long lifetime = -1;

	private boolean renewable = false;

	private boolean inspect = false;

	private String siteName;

	private String output;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_LIFETIME)
				.longOpt(OPT_LIFETIME_LONG)
				.desc("Initial lifetime for token.")
				.hasArg()
				.argName("Lifetime")
				.required(false)
				.get());
		getOptions().addOption(Option.builder("R")
				.longOpt("renewable")
				.desc("Token can be used to get a fresh token.")
				.required(false)
				.get());
		getOptions().addOption(Option.builder("I")
				.longOpt("inspect")
				.desc("Inspect the issued token")
				.required(false)
				.get());
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Site name")
				.required(false)
				.argName("Site")
				.hasArg()
				.get());
		getOptions().addOption(Option.builder("O")
				.longOpt("output-file")
				.desc("File to write the token to")
				.required(false)
				.argName("OutputFile")
				.hasArg()
				.get());
	}

	@Override
	public String getName(){
		return "issue-token";
	}

	@Override
	public String getArgumentList(){
		return "URL";
	}

	@Override
	public String getSynopsis(){
		return "Gets a JWT authentication token from a UNICORE token endpoint. "
				+" This token can be used to authenticate to UNICORE instead of the original credentials. "
				+ "Lifetime and other properties can be configured.";
	}

	@Override
	public String getDescription(){
		return "issue an authentication token";
	}

	@Override
	public void process() throws Exception {
		super.process();
		siteName = getOption(OPT_SITENAME_LONG, OPT_SITENAME);
		String url;
		if(getCommandLine().getArgs().length>1) {
			if(siteName!=null) {
				throw new IllegalArgumentException("Please give only one of --sitename or token endpoint URL!");
			}
			url = getCommandLine().getArgs()[1];
		}else {
			url = getTokenEndpoinnt();
		}
		if(getCommandLine().hasOption(OPT_LIFETIME)){
			lifetime = UnitParser.getTimeParser(0).getLongValue(getOption(OPT_LIFETIME_LONG, OPT_LIFETIME));
			console.debug("Requesting lifetime of <{}> seconds.", lifetime);
		}else{
			console.debug("Using site default for token lifetime.");
		}
		renewable = getBooleanOption("renewable", "R");
		inspect = getBooleanOption("inspect", "I");
		output = getOption("output-file", "O");
		URIBuilder b = new URIBuilder(url);
		if(lifetime>0)b.addParameter("lifetime", String.valueOf(lifetime));
		if(renewable)b.addParameter("renewable", "true");
		console.verbose("Requesting token from {}", url);
		try(BaseClient bc = new BaseClient(b.build().toString(),
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
			ClassicHttpResponse res = bc.get(ContentType.TEXT_PLAIN))
		{
			String token = EntityUtils.toString(res.getEntity());
			if(inspect) {
				showDetails(token);
			}
			console.info("{}", token);
			if(output!=null) {
				console.verbose("Writing token to <{}>", output);
				FileUtils.write(new File(output), token, "UTF-8");
			}
			lastToken = token;
		}
	}

	private String getTokenEndpoinnt() throws Exception {
		String tokenURL = null;
		CoreEndpointLister cl = new CoreEndpointLister(registry,
				configurationProvider, configurationProvider.getRESTAuthN(),
				UCC.executor);
		if(siteName!=null)cl.setAddressFilter(new SiteNameFilter(siteName));
		CoreClient cc = cl.iterator().next();
		if(cc==null) {
			throw new Exception("No site found! Please use --sitename, or give a token endpoint URL.");
		}
		else {
			tokenURL = cc.getEndpoint()+"/token";
		}
		return tokenURL;
	}

	private void showDetails(String token) throws Exception {
		JSONObject o = JWTUtils.getPayload(token);
		String jti = o.optString("jti", null);
		if(jti!=null) {
			console.info("Token ID:     {}", jti);
		}
		String sub = o.getString("sub");
		String uid = o.optString("uid", null);
		if(uid!=null) {
			sub = sub + " (uid="+uid+")";
		}
		String prefs = o.optString("preferences", null);
		if(prefs!=null) {
			sub = sub + " ["+prefs+"]";
		}
		long lt = o.getLong("exp")-o.getLong("iat");
		console.info("Subject:      {}", sub);
		console.info("Lifetime (s): {} ({})", lt, UnitParser.getTimeParser(0).getHumanReadable(lt));
		console.info("Issued by:    {}", o.getString("iss"));
		console.info("Valid for:    {}", o.optString("aud", "<unlimited>"));
		console.info("Renewable:    {}", o.optString("renewable", "no"));
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_UTILITY;
	}

	static String lastToken;

}
