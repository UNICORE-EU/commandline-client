package eu.unicore.ucc.actions.data;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.cli.OptionBuilder;

import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.ucc.authn.UCCConfigurationProvider;
import de.fzj.unicore.ucc.helpers.DefaultMessageWriter;
import de.fzj.unicore.ucc.helpers.ResourceCache;
import eu.unicore.client.Endpoint;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.io.Location;
import eu.unicore.ucc.lookup.IResolve;

/**
 * resolves a remote SMS location given via some "abstract" form or that require lookup
 * 
 * @see Location
 *
 * @author schuller
 */
public class Resolve extends ActionBase {

	protected Location targetDesc;

	protected boolean full;
	
	private final static List<IResolve>resolvers = new ArrayList<>();
	
	static {
		MessageWriter msg = new DefaultMessageWriter();
		try{
			ServiceLoader<IResolve> authn=ServiceLoader.load(IResolve.class);
			for(IResolve p: authn){
				addResolver(p);
			}
		}catch(Exception ex){
			msg.error("Could not load URI resolver(s)", ex);
		}
	}
	/**
	 * manually register a resolver
	 * @param uriScheme - the URI scheme that the resolver can handle. This is only used
	 * internally as a hash key
	 * @param resolver - the resolver
	 */
	public synchronized static void addResolver(IResolve resolver){
		if(!resolvers.contains(resolver))resolvers.add(resolver);
	}
	
	private final static ResourceCache cache = ResourceCache.getInstance();
	
	@Override
	@SuppressWarnings("all")
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(OptionBuilder.withLongOpt("full")
				.withDescription("Print full file URI")
				.isRequired(false)
				.create("f")
		);
		getOptions().addOption(OptionBuilder.withLongOpt("list")
				.withDescription("List all available resolvers / URL schemes")
				.isRequired(false)
				.create("l")
		);
	}
	/**
	 * resolve the given URI using the registered resolvers
	 * 
	 * @param uri
	 * @param registry
	 * @param security
	 * @param messageWriter
	 * @return Location object corresponding to the given URI
	 */
	public static Location resolve(String uri, IRegistryClient registry, UCCConfigurationProvider security, 
			MessageWriter messageWriter){
		String cached = (String)cache.get("_locations_", uri);
		if(cached!=null) {
			return new Location(cached);
		}
		for(IResolve r: resolvers){
			Location loc=r.resolve(uri,registry,security,messageWriter);
			if(loc!=null) {
				// cache it
				cache.put("_locations_", uri, loc.getUnicoreURI());
				return loc;
			}
		}
		return new Location(uri);
	}
	
	@Override
	public void process(){
		super.process();
		if(getCommandLine().hasOption("l")) {
			doList();
		}
		else {
			doResolve();
		}
	}
	
	protected void doList() {
		message("Configured resolvers");
		for(IResolve r: resolvers) {
			message(" * "+r.synopsis());
		}
	}

	protected void doResolve() {
		String target = getCommandLine().getArgs()[1];;
		targetDesc = resolve(target,registry,configurationProvider,this);
		full = getBooleanOption("full", "f");
		Endpoint e = new Endpoint(targetDesc.getSmsEpr());
		try{
			verbose("SMS = "+targetDesc.getSmsEpr());
		}catch(Exception ex){
			error("Can't contact storage service.",ex);
			endProcessing(ERROR);
		}
		String result = full? targetDesc.getUnicoreURI() : e.getUrl();
		message(result);
		if(result!=null)properties.put(PROP_LAST_RESOURCE_URL, result);
	}
	
	@Override
	public String getName() {
		return "resolve";
	}

	@Override
	public String getSynopsis() {
		return "Resolves a remote location and prints the storage URL. "
				+ "Use the '-f' option to print the full UNICORE file URI ";
	}
	
	@Override
	public String getDescription(){
		return "resolve remote location";
	}
	
	@Override
	public String getArgumentList(){
		return "[URI]";
	}
	@Override
	public String getCommandGroup(){
		return "Data management";
	}

}
