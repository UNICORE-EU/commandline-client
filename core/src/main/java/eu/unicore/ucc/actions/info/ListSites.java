package eu.unicore.ucc.actions.info;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.client.core.SiteClient;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.shell.URLCompleter;
import eu.unicore.ucc.lookup.SiteLister;
import eu.unicore.ucc.util.JSONUtil;

public class ListSites extends ListActionBase<SiteClient> {

	private String siteName;

	public String getName(){
		return "list-sites";
	}
	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_SITENAME)
				.longOpt(OPT_SITENAME_LONG)
				.desc("Site Name")
				.required(false)
				.argName("Site")
				.hasArg()
				.build());
	}
	
	@Override
	public void process() throws Exception {
		super.process();
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);
		SiteLister tssLister = new SiteLister(UCC.executor,registry,configurationProvider);
		for(SiteClient c: tssLister){
			try{
				if(c==null){
					if(!tssLister.isRunning()){
						break;
					}
					else try{
						Thread.sleep(100);
					}catch(Exception ex) {}
				}
				else if(filterMatch(c)){
					String url = c.getEndpoint().getUrl();
					URLCompleter.registerSiteURL(url);
					if(!siteNameMatches(siteName, url))continue;
					listSite(c);
					properties.put(PROP_LAST_RESOURCE_URL, url);
					lastNumberOfResults++;
				}
			}catch(Exception ex){
				if(c!=null && c.getEndpoint()!=null) {
					console.error(ex, "Error listing site at <{}>", c.getEndpoint().getUrl());
				}
				else console.error(ex, "");
			}
		}
	}

	private void listSite(SiteClient site)throws Exception{
		properties.put(PROP_LAST_RESOURCE_URL, site.getEndpoint().getUrl());
		console.info("{} {} {}", site.getProperties().getString("siteName"),
				site.getEndpoint().getUrl(), getDetails(site));
		printProperties(site);
	}

	public static final String appSeparator = "---v";

	@Override
	protected String getDetails(SiteClient tss)throws Exception{
		if(!detailed)return "";
		StringBuilder details=new StringBuilder();
		String sep=System.getProperty("line.separator");
		JSONObject props = tss.getProperties();
		details.append(sep).append("  Number of jobs: ").append(props.get("numberOfJobs"));

		boolean first=true;
		for(String a: JSONUtil.asList(props.getJSONArray("applications"))){
			if(!first){
				details.append(", ");
			}
			else {
				details.append(sep).append("  Applications: ");
				first=false;
			}
			String name = a.split(appSeparator)[0];
			String version = a.split(appSeparator)[1];
			details.append(name).append(" ").append(version);
		}
		details.append(sep).append("  Resources: ");
		listResources(props.getJSONObject("resources"), details);
		try {
			Map<String,String> budget = JSONUtil.asMap(props.getJSONObject("remainingComputeTime"));
			details.append(sep).append("  Compute time:");
			if(budget.isEmpty())details.append(": n/a").append(sep);
			for(String project: budget.keySet()) {
				details.append(sep).append("     "+project).append(": ").append("...");
			}
		}catch(Exception ex) {}
		return details.toString();
	}

	private void listResources(JSONObject resources, StringBuilder details){
		String sep=System.getProperty("line.separator");
		try{
			Iterator<String> resIterator = resources.keys();
			while(resIterator.hasNext()) {
				String name = resIterator.next();
				details.append(sep).append("     ").
				    append(name).append(": ").
				    append(resources.getString(name));
			}
		}
		catch(Exception ex){}
	}

	@Override
	public String getDescription(){
		return "list remote job execution sites";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_JOBS;
	}

	@Override
	public String getSynopsis() {
		return "List the UNICORE job execution sites available to you, together " +
				"with some information about the site's capabilities. "
				+"Use the -l or -a options to control the amount of information. "
				+"Use the -s option to limit the list to a single site.";
	}

}
