package eu.unicore.ucc.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.fzj.unicore.uas.util.MessageWriter;
import eu.unicore.client.Endpoint;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.rest.client.Resources;

/**
 * a more flexible version of a multi-registry client where the
 * individual registries need not be UNICORE RegistryClient instances
 * 
 * This is read-only, i.e. you cannot add to the registries using this 
 * client
 * 
 * @author schuller
 */
public class MultiRegistryClient implements IRegistryClient {

	private final List<IRegistryClient>clients = new ArrayList<IRegistryClient>();

	private boolean filterDuplicates=true;

	private String connectionStatus=null;

	private final MessageWriter msg;

	public MultiRegistryClient(MessageWriter msg){
		this.msg=msg;
	}

	public void addRegistry(IRegistryClient registry){
		clients.add(registry);
	}
	
	@Override
	public List<Endpoint> listEntries(ServiceListFilter acceptFilter) throws Exception {
		List<Endpoint>result=new ArrayList<>();
		for(IRegistryClient c: clients){
			try{
				List<Endpoint>res = c.listEntries(acceptFilter);
				if(filterDuplicates){
					addIfNotExist(result, res);
				}else{
					result.addAll(res);
				}
			}catch(Exception ex){
				msg.verbose("Registry at "+getAddress(c)+" is not available: "+ex.getMessage());
			}
		}
		return result;
	}

	@Override
	public List<Endpoint> listEntries() throws Exception {
		List<Endpoint>result=new ArrayList<>();
		for(IRegistryClient c: clients){
			try{
				List<Endpoint>res = c.listEntries();
				if(filterDuplicates){
					addIfNotExist(result, res);
				}else{
					result.addAll(res);
				}
			}catch(Exception ex){
				msg.verbose("Registry at "+getAddress(c)+" is not available: "+ex.getMessage());
			}
		}
		return result;
	}

	@Override
	public List<Endpoint> listEntries(String type) throws Exception {
		return listEntries(new RegistryClient.ServiceTypeFilter(type));
	}
	public String getConnectionStatus(){
		checkConnection();
		return connectionStatus;		
	}

	private void addIfNotExist(List<Endpoint>target, List<Endpoint>source){
		Set<String>addresses=new HashSet<String>();
		for(Endpoint epr: target){
			addresses.add(epr.getUrl());
		}
		for(Endpoint e: source){
			String address=e.getUrl();
			if(!addresses.contains(address)){
				addresses.add(address);
				target.add(e);
			}
		}
	}

	public boolean isFilterDuplicates() {
		return filterDuplicates;
	}

	public void setFilterDuplicates(boolean filterDuplicates) {
		this.filterDuplicates = filterDuplicates;
	}

	private Boolean compute(Callable<Boolean>task, int timeout){
		try{
			Future<Boolean>f = Resources.getExecutorService().submit(task);
			return f.get(timeout, TimeUnit.MILLISECONDS);
		}catch(Exception ex){
			return Boolean.FALSE;
		}
	}

	/**
	 * check the connection to the services. If the service does
	 * not reply within a fixed 2 second timeout, returns <code>false</code>
	 */
	public boolean checkConnection(){
		return checkConnection(2000);
	}

	/**
	 * check the connection to the services. If no service 
	 * replies within the given timeout, returns <code>false</code>
	 * 
	 * @param timeout - connection timeout in milliseconds
	 */
	public boolean checkConnection(int timeout){
		final StringBuffer status=new StringBuffer();
		boolean result=false;
		for(final IRegistryClient c: clients){
			Callable<Boolean>task=new Callable<Boolean>(){
				public Boolean call()throws Exception{
					return Boolean.valueOf(c.checkConnection());
				}
			};

			Boolean res=compute(task, timeout);
			boolean currentOK=res!=null?res.booleanValue():false;
			if(!currentOK){
				status.append("[NOT AVAILABLE: ").append(getAddress(c));
				status.append("] ");
			}
			result=result || currentOK;

		}
		if(result)connectionStatus="OK";
		else connectionStatus=status.toString();

		return result;
	}

	String getAddress(IRegistryClient c){
		if(c instanceof RegistryClient){
			return ((RegistryClient)c).getEndpoint().getUrl();
		}
		else{
			//some non-standard Registry impl
			return c.getClass().getName();
		}
	}

}
