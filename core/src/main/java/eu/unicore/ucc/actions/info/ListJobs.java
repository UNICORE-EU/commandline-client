package eu.unicore.ucc.actions.info;

import java.util.Date;

import org.apache.commons.cli.Option;

import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.lookup.CoreEndpointLister;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.actions.shell.URLCompleter;
import eu.unicore.ucc.lookup.JobLister;

/**
 * creates a job listing
 * 
 * @author schuller
 */
public class ListJobs extends ListActionBase<JobClient> {

	private String siteName;

	public String getName(){
		return "list-jobs";
	}

	@Override
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
		CoreEndpointLister siteLister = new CoreEndpointLister(registry, configurationProvider,
				configurationProvider.getRESTAuthN(), UCC.executor);
		if(detailed)printHeader();
		for(CoreClient c: siteLister){
			if(c==null){
				if(!siteLister.isRunning()){
					break;
				}
			}
			else{
				String siteURL = c.getEndpoint().getUrl();
				try{
					if(isBlacklisted(siteURL))continue;
					URLCompleter.registerSiteURL(siteURL);
					if(!siteNameMatches(siteName, siteURL))continue;
					for(JobClient job: new JobLister(c, tags)){
						if(filterMatch(job)){
							listJob(job);
							lastNumberOfResults++;
						}
					}
				}catch(Exception ex){
					String msg="Error accessing TSS at "+siteURL;
					logger.error(msg,ex);
					console.verbose("{}", msg);
				}
			}
		}
	}

	private void listJob(JobClient job) throws Exception {
		properties.put(PROP_LAST_RESOURCE_URL, job.getEndpoint().getUrl());
		if(detailed) {
			console.info("{}", getDetails(job));
		}
		else {
			console.info("{}", job.getEndpoint().getUrl());
		}
		printProperties(job);
	}

	final String format = " %16s | %10s | %s";

	private void printHeader() {
		console.info(String.format(format, "Submitted", "Status", "URL"));
		console.info(" -----------------|------------|----------------");
	}

	@Override
	protected String getDetails(JobClient job){
		try	{
			Date sTime = UnitParser.getISO8601().parse(job.getSubmissionTime());
			String t = UnitParser.getSimpleDateFormat().format(sTime);
			return String.format(format, t, job.getStatus(), job.getEndpoint().getUrl());
		}catch(Exception ex){
			return " ERROR: "+ex.getMessage();
		}
	}

	@Override
	public String getDescription(){
		return "list your jobs";
	}

	@Override
	public String getSynopsis(){
		return "Lists your jobs per target system. " +
				"The list can be limited to a single target system specified " +
				"using the '-s' option.";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_JOBS;
	}
}
