package eu.unicore.ucc.io;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.ucc.UCC;
import eu.unicore.client.data.UFTPConstants;
import eu.unicore.util.Log;

public class FiletransferParameterProvider implements
de.fzj.unicore.uas.FiletransferParameterProvider {

	@Override
	public void provideParameters(Map<String, String> params, String protocol) {
		MessageWriter msg=UCC.getMessageWriter();

		if("UFTP".equals(protocol)){
			//setup client IP for UFTP
			String hostSpec=params.get(UFTPConstants.PARAM_CLIENT_HOST);
			if(hostSpec==null || "auto".equalsIgnoreCase(hostSpec)){
				msg.verbose("UFTP: parameter <"+UFTPConstants.PARAM_CLIENT_HOST+"> will be determined automatically.");
				params.remove(UFTPConstants.PARAM_CLIENT_HOST);
			} else {
				String ipList = getAllHostIPs(hostSpec);
				msg.verbose("UFTP: parameter <"+UFTPConstants.PARAM_CLIENT_HOST+"> determined as <"+ipList+">");
				params.put(UFTPConstants.PARAM_CLIENT_HOST, ipList);
			}
			//number of streams
			String streams=params.get(UFTPConstants.PARAM_STREAMS);
			if(streams==null){
				msg.verbose("UFTP: parameter <"+UFTPConstants.PARAM_STREAMS+"> is not set, will use default value of <1>");
				params.put(UFTPConstants.PARAM_STREAMS,"1");
			}
			if(params.get(UFTPConstants.PARAM_SECRET)==null) {
				//secret -- must be a unique string
				params.put(UFTPConstants.PARAM_SECRET, UUID.randomUUID().toString());
			}
		}
	}

	protected String getAllHostIPs(String ipSpec){
		String clientIP = ipSpec;
		if("all".equalsIgnoreCase(ipSpec)){
			try{
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
				clientIP = sb.toString();
			}catch(Exception e){
				System.err.println(Log.createFaultMessage("WARNING:", e));
			}
		}
		return clientIP;
	}
	
}
