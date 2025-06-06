package eu.unicore.ucc.actions;

import org.apache.commons.cli.Option;
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
import eu.unicore.ucc.UCC;

/**
 * Call a /rest/token endpoint to issue a JWT token 
 *
 * @author schuller
 */
public class IssueToken extends ActionBase {

	// token lifetime (in seconds)
	private int lifetime;;

	private boolean limited=false;

	private boolean renewable=false;

	private boolean inspect=false;

	private String siteName;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_LIFETIME)
				.longOpt(OPT_LIFETIME_LONG)
				.desc("Initial lifetime (in seconds) for token.")
				.argName("Lifetime")
				.hasArg()
				.required(false)
				.build());
		getOptions().addOption(Option.builder("L")
				.longOpt("limited")
				.desc("Token should be limited to the issuing server")
				.required(false)
				.build());
		getOptions().addOption(Option.builder("R")
				.longOpt("renewable")
				.desc("Token can be used to get a fresh token.")
				.required(false)
				.build());
		getOptions().addOption(Option.builder("I")
				.longOpt("inspect")
				.desc("Inspect the issued token")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Site name")
				.required(false)
				.argName("Site")
				.hasArg()
				.build());
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
			url = resolveSite();
		}
		lifetime=getNumericOption(OPT_LIFETIME_LONG, OPT_LIFETIME, -1);
		if(lifetime>0){
			console.verbose("Requesting lifetime of <{}> seconds.", lifetime);
		}else{
			console.verbose("Using site default for token lifetime.");
		}
		limited = getBooleanOption("limited", "L");
		renewable = getBooleanOption("renewable", "R");
		inspect = getBooleanOption("inspect", "I");
		URIBuilder b = new URIBuilder(url);
		if(lifetime>0)b.addParameter("lifetime", String.valueOf(lifetime));
		if(renewable)b.addParameter("renewable", "true");
		if(limited)b.addParameter("limited", "true");
		BaseClient bc = new BaseClient(b.build().toString(),
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
		console.verbose("Requesting token from {}", bc.getURL());
		try(ClassicHttpResponse res = bc.get(ContentType.TEXT_PLAIN)){
			String token = EntityUtils.toString(res.getEntity());
			if(inspect) {
				showDetails(token);
			}
			console.info("{}", token);
			lastToken = token;
		}
	}

	private String resolveSite() throws Exception {
		CoreEndpointLister cl = new CoreEndpointLister(registry,
				configurationProvider, configurationProvider.getRESTAuthN(),
				UCC.executor);
		if(siteName!=null)cl.setAddressFilter(new SiteNameFilter(siteName));
		CoreClient cc = cl.iterator().next();
		if(cc==null) {
			throw new Exception("No site found! Please use --sitename, or give a token endpoint URL.");
		}
		return cc.getEndpoint().getUrl()+"/token";
	}

	private void showDetails(String token) throws Exception {
		JSONObject o = JWTUtils.getPayload(token);
		String sub = o.getString("sub");
		String uid = o.optString("uid", null);
		if(uid!=null) {
			sub = sub + " (uid="+uid+")";
		}
		console.info("Subject:      {}", sub);
		console.info("Lifetime (s): {}", o.getInt("exp")-o.getInt("iat"));
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
