package eu.unicore.ucc.actions.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.unicore.client.admin.AdminServiceClient;
import eu.unicore.client.admin.AdminServiceClient.AdminCommand;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.ucc.actions.info.ListActionBase;
import eu.unicore.ucc.lookup.SiteFilter;

/**
 * lists accessible admin services and shows information
 *
 * @author schuller
 */
public class AdminServiceInfo extends ListActionBase<AdminServiceClient>{

	@Override
	protected Iterable<AdminServiceClient>iterator()throws Exception {
		List<AdminServiceClient>clients = new ArrayList<>();
		List<String> urls = findURLs();
		for(String u: urls){
			AdminServiceClient asc = new AdminServiceClient(u,
					configurationProvider.getClientConfiguration(u),
					configurationProvider.getRESTAuthN());
			clients.add(asc);
		}
		return clients;
	}

	@Override
	protected void list(AdminServiceClient asc)throws Exception{
		console.info("{} {}", asc.getEndpoint(), getDetails(asc));
		printProperties(asc);
	}

	@Override
	protected String getDetails(AdminServiceClient asc)throws Exception{
		StringBuilder details = new StringBuilder();
		for(AdminCommand ac: asc.getCommands()) {
			if(details.length()==0){
				details.append(_newline).append("  Commands: ");
			}
			details.append(_newline).append("    ");
			details.append(ac.name);
			// store for shell completer
			commands.add(ac.name);
			details.append(" : ").append(ac.description);
		}
		return detailed? details.toString() : "";
	}

	private List<String> findURLs()throws Exception{
		SiteFilter f = new SiteFilter(siteName, blacklist);
		List<String>tsfs = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
		List<String>result = new ArrayList<>();
		for(String tsfURL: tsfs){
			if(!f.accept(tsfURL))continue;
			int endIndex = tsfURL.lastIndexOf("/core");
			result.add(tsfURL.substring(0, endIndex)+"/admin");
		}
		return result;
	}

	@Override
	public String getName() {
		return "admin-info";
	}

	@Override
	public String getSynopsis() {
		return "Displays accessible admin service instances and some information about them.";
	}

	@Override
	public String getDescription() {
		return "show accessible admin service instances";
	}

	@Override
	public String getCommandGroup() {
		return CMD_GRP_ADMIN;
	}

	static Set<String> commands = new HashSet<>();

}
