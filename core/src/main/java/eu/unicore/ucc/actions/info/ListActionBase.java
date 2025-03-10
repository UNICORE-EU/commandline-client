package eu.unicore.ucc.actions.info;

import java.util.Arrays;

import org.apache.commons.cli.Option;

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
	
	protected boolean raw;
	
	protected boolean doFilter;
	
	protected Filter filter;
	
	protected String[] tags;
	
	protected String[] fields;
	
	//for unit testing
	protected static int lastNumberOfResults;
	
	@Override
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(Option.builder(OPT_FILTER)
				.longOpt(OPT_FILTER_LONG)
				.desc("Filter the list")
				.required(false)
				.hasArgs()
				.build());
		getOptions().addOption(Option.builder(OPT_DETAILED)
				.longOpt(OPT_DETAILED_LONG)
				.desc("Detailed output")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_RAW)
				.longOpt(OPT_RAW_LONG)
				.desc("Print all properties in JSON format")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_FIELDS)
				.longOpt(OPT_FIELDS_LONG)
				.desc("Print only the named fields")
				.required(false)
				.hasArgs()
				.build());	
		getOptions().addOption(Option.builder(OPT_TAGS)
				.longOpt(OPT_TAGS_LONG)
				.desc("Only list items with the given tags")
				.required(false)
				.hasArgs()
				.valueSeparator(',')
				.build());
	}

	@Override
	public void process() throws Exception {
		super.process();
		lastNumberOfResults=0;
		doFilter=getCommandLine().hasOption(OPT_FILTER);
		console.verbose("Filtering = {}", doFilter);
		detailed=getBooleanOption(OPT_DETAILED_LONG, OPT_DETAILED);
		console.verbose("Detailed listing = {}", detailed);
		raw=getBooleanOption(OPT_RAW_LONG, OPT_RAW);
		
		if(doFilter){
			filter=createFilter(getCommandLine().getOptionValues(OPT_FILTER));
		}
		if(getCommandLine().hasOption(OPT_TAGS)){
			tags = getCommandLine().getOptionValues(OPT_TAGS);
			console.verbose("Tags = {}", Arrays.deepToString(tags));
		}
		if(getCommandLine().hasOption(OPT_FIELDS)){
			fields = getCommandLine().getOptionValues(OPT_FIELDS);
			console.verbose("Fields = {}", Arrays.asList(fields));
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
	 * print the properties of a list entry
	 * 
	 * @param entry - the BaseServiceClient for the current list entry
	 */
	protected void printProperties(T entry) throws Exception {
		if(raw || (fields!=null && fields.length>0)){
			console.info("{}", entry.getProperties(fields).toString(2));
		}
	}
	
	protected Filter createFilter(String[] args){
		filter = PropertyFilter.create(args);
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
