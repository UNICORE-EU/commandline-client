package eu.unicore.ucc.actions.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.unicore.client.Endpoint;
import eu.unicore.client.admin.AdminServiceClient;
import eu.unicore.client.admin.AdminServiceClient.AdminCommand;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.ucc.actions.info.ListActionBase;

/**
 * lists accessible admin services and shows information
 *
 * @author schuller
 */
public class AdminServiceInfo extends ListActionBase<BaseServiceClient>{

	@Override
	public void process() throws Exception {
		super.process();
		List<AdminServiceClient>clients = createClients();
		for(AdminServiceClient asc: clients){
			try{
				console.verbose("Contacting admin service at <{}>", asc.getEndpoint().getUrl());
			}catch(Exception ex){
				console.error(ex, "Can't access <{}>", asc.getEndpoint().getUrl());
				continue;
			}
			if(filterMatch(asc)){
				try{
					list(asc);
					lastNumberOfResults++;
				}catch(Exception ex){
					console.error(ex, "Error showing admin service info at <{}>", asc.getEndpoint().getUrl());
				}
			}						
		}
	}

	private void list(AdminServiceClient asc)throws Exception{
		console.info("{} {}", asc.getEndpoint().getUrl(), getDetails(asc));
		printProperties(asc);
	}

	private String getDetails(AdminServiceClient asc)throws Exception{
		if(!detailed)return "";
		StringBuilder details=new StringBuilder();
		String sep=System.getProperty("line.separator");
		boolean first=true;

		Map<String,String> allMetrics = asc.getMetrics();

		first=true;
		for(String m: allMetrics.keySet()){
			if(first){
				details.append(sep).append("  Metrics: ");
				first=false;
			}
			details.append(sep).append("    ");
			details.append(m);
			details.append(": ").append(allMetrics.get(m));
		}
		first=true;
		for(AdminCommand ac: asc.getCommands()) {
			if(first){
				details.append(sep).append("  Commands: ");
				first=false;
			}
			details.append(sep).append("    ");
			details.append(ac.name);
			details.append(" : ").append(ac.description);
		}

		return details.toString();
	}




	private List<AdminServiceClient> createClients()throws Exception{
		List<AdminServiceClient>clients = new ArrayList<>();
		List<Endpoint> urls = findURLs();
		for(Endpoint u: urls){
			AdminServiceClient asc = new AdminServiceClient(u,
					configurationProvider.getClientConfiguration(u.getUrl()),
					configurationProvider.getRESTAuthN());
			clients.add(asc);
		}
		return clients;
	}

	private List<Endpoint> findURLs()throws Exception{
		List<Endpoint>tsfs = registry.listEntries(new RegistryClient.ServiceTypeFilter("TargetSystemFactory"));
		List<Endpoint>result = new ArrayList<>();

		for(Endpoint epr: tsfs){
			String tsfURL = epr.getUrl();
			int endIndex = tsfURL.lastIndexOf("/core/factories/");
			String adminServiceURL=tsfURL.substring(0, endIndex)+"/admin";
			if(isBlacklisted(adminServiceURL))continue;
			result.add( new Endpoint(adminServiceURL));
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

}
