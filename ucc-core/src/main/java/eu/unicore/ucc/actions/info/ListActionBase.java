package eu.unicore.ucc.actions.info;

import java.util.Arrays;

import org.apache.commons.cli.OptionBuilder;

import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.lookup.Filter;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.lookup.PropertyFilter;
/**
 * Base class for actions creating a listing.<br/>
 * 
 * It supports filtering the resulting list.
 * 
 * @author schuller
 */
public abstract class ListActionBase<T extends BaseServiceClient> extends ActionBase {
	
	protected boolean detailed;
	
	protected boolean all;
	
	protected boolean doFilter;
	
	protected Filter filter;
	
	protected String[] tags;
	
	protected String[] fields;
	
	//for unit testing
	public static int lastNumberOfResults;
	
	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_FILTER_LONG)
				.withDescription("Filter the list")
				.isRequired(false)
				.hasArgs()
				.create(OPT_FILTER)
			);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_DETAILED_LONG)
				.withDescription("Detailed output")
				.isRequired(false)
				.create(OPT_DETAILED)
			);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_ALL_LONG)
				.withDescription("Print all properties")
				.isRequired(false)
				.create(OPT_ALL)
			);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_FIELDS_LONG)
				.withDescription("Print only the named fields")
				.isRequired(false)
				.hasArgs()
				.create(OPT_FIELDS)
			);
		getOptions().addOption(OptionBuilder.withLongOpt(OPT_TAGS_LONG)
				.withDescription("Only list items with the given tags")
				.isRequired(false)
				.hasArgs()
				.create(OPT_TAGS)
			);		
	}

	@Override
	public void process() {
		super.process();
		lastNumberOfResults=0;
		doFilter=getCommandLine().hasOption(OPT_FILTER);
		verbose("Filtering = "+doFilter);
		detailed=getBooleanOption(OPT_DETAILED_LONG, OPT_DETAILED);
		verbose("Detailed listing = "+detailed);
		all=getBooleanOption(OPT_ALL_LONG, OPT_ALL);
		
		if(doFilter){
			filter=createFilter(getCommandLine().getOptionValues(OPT_FILTER));
		}
		if(getCommandLine().hasOption(OPT_TAGS)){
			tags = getCommandLine().getOptionValues(OPT_TAGS);
			verbose("Tags = "+Arrays.asList(tags));
		}
		if(getCommandLine().hasOption(OPT_FIELDS)){
			fields = getCommandLine().getOptionValues(OPT_FIELDS);
			verbose("Fields = "+Arrays.asList(fields));
		}
	}

	
	/**
	 * Print details about a list entry.</br> 
	 * The default implementation returns an empty string
	 * 
	 * @param entry - the {@link BaseServiceClient} for the current list entry
	 */
	protected String getDetails(T entry)throws Exception{
		return "";
	}

	
	/**
	 * print details about a list entry, the 
	 * default implementation prints the properties document
	 * 
	 * @param entry - the BaseServiceClient for the current list entry
	 */
	protected void printProperties(T entry){
		if(all || (fields!=null && fields.length>0)){
			try{
				message(entry.getProperties(fields).toString(2));
			}
			catch(Exception e){
				error("Could not get resource properties!", e);
				endProcessing(ERROR);
			}
		}
	}
	
	protected Filter createFilter(String[] args){
		filter=PropertyFilter.Factory.create(this, args);
		if(filter==null){
			throw new IllegalArgumentException("Filter specification <"+Arrays.asList(args)+"> not understood.");
		}
		return filter;
	}
	
	/**
	 * if filtering is enabled, the given resource is checked. 
	 * 
	 * @param resource
	 * @return true if the configured filter matches the resource 
	 */
	protected boolean filterMatch(T resource) throws Exception {
		if(isBlacklisted(resource.getEndpoint().getUrl()))return false;
		if(!doFilter)return true;
		return filter.accept(resource);
	}
	
	protected boolean siteNameMatches(String name, String url) {
		return name==null || (url!=null && url.contains("/"+name+"/"));
	}
	
	//for unit testing
	public static int getLastNumberOfResults(){
		return lastNumberOfResults;
	}
}
