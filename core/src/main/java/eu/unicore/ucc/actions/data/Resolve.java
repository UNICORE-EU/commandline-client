package eu.unicore.ucc.actions.data;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.cli.Option;

import eu.unicore.client.Endpoint;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.ucc.actions.ActionBase;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.helpers.ResourceCache;
import eu.unicore.ucc.io.Location;
import eu.unicore.ucc.lookup.IResolve;

/**
 * resolves a remote SMS location given via some "abstract" form or that require lookup
 *
 * @author schuller
 */
public class Resolve extends ActionBase {

	private Location targetDesc;

	private boolean full;

	private final static List<IResolve>resolvers = new ArrayList<>();

	static {
		ServiceLoader<IResolve> authn=ServiceLoader.load(IResolve.class);
		for(IResolve p: authn){
			addResolver(p);
		}
	}
	/**
	 * manually register a resolver
	 * @param resolver - the resolver
	 */
	public synchronized static void addResolver(IResolve resolver){
		if(!resolvers.contains(resolver))resolvers.add(resolver);
	}

	private final static ResourceCache cache = ResourceCache.getInstance();

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder("f")
				.longOpt("full")
				.desc("Print full file URI")
				.required(false)
				.build());
		getOptions().addOption(Option.builder("l")
				.longOpt("list")
				.desc("List all available resolvers / URL schemes")
				.required(false)
				.build());

	}
	/**
	 * resolve the given URI using the registered resolvers
	 *
	 * @param uri
	 * @param registry
	 * @param security
	 * @return Location object corresponding to the given URI
	 */
	public static Location resolve(String uri, IRegistryClient registry, UCCConfigurationProvider security){
		String cached = (String)cache.get("_locations_", uri);
		if(cached!=null) {
			return new Location(cached);
		}
		for(IResolve r: resolvers){
			Location loc=r.resolve(uri,registry,security);
			if(loc!=null) {
				cache.put("_locations_", uri, loc.getUnicoreURI());
				return loc;
			}
		}
		return new Location(uri);
	}

	@Override
	public void process() throws Exception {
		super.process();
		if(getCommandLine().hasOption("l")) {
			doList();
		}
		else {
			doResolve();
		}
	}

	private void doList() {
		console.info("Configured resolvers");
		for(IResolve r: resolvers) {
			console.info(" * {}", r.synopsis());
		}
	}

	private void doResolve() throws Exception {
		String target = getCommandLine().getArgs()[1];;
		targetDesc = resolve(target,registry,configurationProvider);
		full = getBooleanOption("full", "f");
		Endpoint e = new Endpoint(targetDesc.getSmsEpr());
		console.verbose("SMS = {}", targetDesc.getSmsEpr());
		String result = full? targetDesc.getUnicoreURI() : e.getUrl();
		console.info("{}", result);
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
		return CMD_GRP_DATA;
	}

}
