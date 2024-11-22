package eu.unicore.ucc.io;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

import eu.unicore.client.data.UFTPConstants;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.helpers.ConsoleLogger;

public class FiletransferParameterProvider implements
eu.unicore.uas.FiletransferParameterProvider {

	@Override
	public void provideParameters(Map<String, String> params, String protocol) {
		ConsoleLogger msg = UCC.getConsoleLogger();

		if("UFTP".equals(protocol)){
			// setup client IP for UFTP
			String hostSpec=params.get(UFTPConstants.PARAM_CLIENT_HOST);
			if(hostSpec==null || "auto".equalsIgnoreCase(hostSpec)){
				msg.verbose("UFTP: parameter <"+UFTPConstants.PARAM_CLIENT_HOST+"> will be determined automatically.");
				params.remove(UFTPConstants.PARAM_CLIENT_HOST);
			} else if("all".equalsIgnoreCase(hostSpec)){
				try {
					String ipList = getAllHostIPs(hostSpec);
					msg.verbose("UFTP: parameter <"+UFTPConstants.PARAM_CLIENT_HOST+"> determined as <"+ipList+">");
					params.put(UFTPConstants.PARAM_CLIENT_HOST, ipList);
				}catch(Exception e) {
					msg.error("Error determining network address(es), falling back to 'auto' mode", e);
					params.remove(UFTPConstants.PARAM_CLIENT_HOST);
				}
			}
			String streams=params.get(UFTPConstants.PARAM_STREAMS);
			if(streams==null){
				msg.verbose("UFTP: parameter <"+UFTPConstants.PARAM_STREAMS+"> is not set, will use default value of <1>");
				params.put(UFTPConstants.PARAM_STREAMS,"1");
			}
			if(params.get(UFTPConstants.PARAM_SECRET)==null) {
				params.put(UFTPConstants.PARAM_SECRET, UUID.randomUUID().toString());
			}
		}
	}

	protected String getAllHostIPs(String ipSpec) throws Exception {
		StringBuilder sb = new StringBuilder();
		Enumeration<NetworkInterface>iter = NetworkInterface.getNetworkInterfaces();
		while(iter.hasMoreElements()){
			NetworkInterface ni = iter.nextElement();
			Enumeration<InetAddress> addresses = ni.getInetAddresses();
			while(addresses.hasMoreElements()){
				InetAddress ia = addresses.nextElement();
				if(sb.length()>0)sb.append(",");
				sb.append(ia.getHostAddress());
			}
		}
		return sb.toString();
	}
	
}
