package eu.unicore.ucc.actions.info;

import java.util.Date;

import eu.unicore.client.core.JobClient;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.lookup.JobLister;
import eu.unicore.ucc.lookup.SiteFilter;

/**
 * creates a job listing
 * 
 * @author schuller
 */
public class ListJobs extends ListActionBase<JobClient> {

	@Override
	public String getName(){
		return "list-jobs";
	}

	@Override
	public void setupOptions() {
		super.setupOptions();
		if(detailed)printHeader();
	}

	@Override
	protected Iterable<JobClient>iterator()throws Exception {
		JobLister jobLister = new JobLister(UCC.executor, registry, configurationProvider, tags);
		jobLister.setAddressFilter(new SiteFilter(siteName, blacklist));
		return () -> jobLister.iterator(); 
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
