package eu.unicore.ucc.actions.info;

import org.apache.commons.cli.Option;

import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.lookup.CoreEndpointLister;
import eu.unicore.ucc.lookup.JobLister;

/**
 * creates a job listing
 * 
 * @author schuller
 */
public class ListJobs extends ListActionBase<JobClient> {

	protected String siteName;

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
	public void process() {
		super.process();
		siteName=getCommandLine().getOptionValue(OPT_SITENAME);
		CoreEndpointLister siteLister = new CoreEndpointLister(registry, configurationProvider,
				configurationProvider.getRESTAuthN());
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
					if(!siteNameMatches(siteName, siteURL))continue;
					
					verbose("Listing site at "+siteURL);
					for(JobClient job: new JobLister(c, tags)){
						if(filterMatch(job)){
							listJob(job);
							lastNumberOfResults++;
						}
					}
				}catch(Exception ex){
					String msg="Error accessing TSS at "+siteURL;
					logger.error(msg,ex);
					verbose(msg);
				}
			}
		}
	}

	protected void listJob(JobClient job){
		properties.put(PROP_LAST_RESOURCE_URL, job.getEndpoint().getUrl());
		message(job.getEndpoint().getUrl()+getDetails(job));
		printProperties(job);
	}


	@Override
	protected String getDetails(JobClient job){
		if(!detailed)return "";

		try	{
			return " "+job.getStatus();
		}catch(Exception ex){
			return " ERROR getting status";
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
	public String getArgumentList(){
		return "";
	}
	@Override
	public String getCommandGroup(){
		return CMD_GRP_JOBS;
	}
}
