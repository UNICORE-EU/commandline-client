package eu.unicore.ucc.actions.info;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.OptionBuilder;
import org.json.JSONObject;

import de.fzj.unicore.ucc.UCC;
import de.fzj.unicore.ucc.util.JSONUtil;
import eu.unicore.client.core.SiteClient;
import eu.unicore.ucc.lookup.SiteLister;

public class ListSites extends ListActionBase<SiteClient> {

	protected String siteName;

	public String getName(){
		return "list-sites";
	}
	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_SITENAME_LONG)
				.withDescription("Site Name")
				.withArgName("Vsite")
				.hasArg()
				.isRequired(false)
				.create(OPT_SITENAME)
				);
	}
	
	@Override
	public void process() {
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
					if(!siteNameMatches(siteName, c.getEndpoint().getUrl()))continue;
					listTSS(c);
					properties.put(PROP_LAST_RESOURCE_URL, c.getEndpoint().getUrl());
					lastNumberOfResults++;
				}
			}catch(Exception ex){
				if(c!=null)error("Error listing site at <"+c.getEndpoint().getUrl()+">",ex);
				else error("",ex);
			}
		}
	}

	protected void listTSS(SiteClient tss)throws Exception{
		message(tss.getProperties().getString("siteName")+" "+tss.getEndpoint().getUrl()+" "+getDetails(tss));
		printProperties(tss);
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

	protected void listResources(JSONObject resources, StringBuilder details){
		String sep=System.getProperty("line.separator");
		try{
			@SuppressWarnings("unchecked")
			Iterator<String> resIterator = resources.keys();
			while(resIterator.hasNext()) {
				String name = resIterator.next();
				details.append(sep).append("     "+name).append(": ").append(resources.getString(name));
			}
		}
		catch(Exception ex){}
	}
	
	@Override
	public String getDescription(){
		return "list remote sites";
	}

	@Override
	public String getArgumentList(){
		return "";
	}
	@Override
	public String getCommandGroup(){
		return "General";
	}
	
	@Override
	public String getSynopsis() {
		return "List the Grid sites available to you, together " +
				"with some information about the site's capabilities. "
				+"Use the -l or -a options to control the amount of information. "
				+"Use the -s option to limit the list to a single site.";
	}

}
