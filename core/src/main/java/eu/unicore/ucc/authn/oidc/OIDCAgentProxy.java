package eu.unicore.ucc.authn.oidc;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.nio.channels.Channels;

import org.json.JSONObject;

import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

/**
 * Connector to the 'oidc-agent' via UNIX domain socket.
 * 
 * Modeled after the JSch ssh-agent proxy
 *  
 * @author schuller
 */
public class OIDCAgentProxy {

	private static final String OIDC_SOCK = "OIDC_SOCK";
	
	public OIDCAgentProxy() {}

	public static boolean isConnectorAvailable(){
		return isConnectorAvailable(null);
	}

	public static boolean isConnectorAvailable(String usocketPath){
		return System.getenv(OIDC_SOCK)!=null || usocketPath!=null;
	}
	
	public String send(String data) throws Exception {
		return send(data,System.getenv(OIDC_SOCK));
	}
	
	public String send(String data, String path) throws Exception {
		UnixSocketAddress addr = new UnixSocketAddress(path);
		UnixSocketChannel channel = UnixSocketChannel.create();
		channel.connect(addr);
        try(PrintWriter w = new PrintWriter(Channels.newOutputStream(channel));
        	InputStreamReader r = new InputStreamReader(Channels.newInputStream(channel)))
        {
        	w.print(data);
        	w.flush();
        	CharBuffer result = CharBuffer.allocate(4096);
        	r.read(result);
        	result.flip();
        	return result.toString();
        }
	}
	
	// testing...
	public static void main(String[] args) throws Exception {
		if(!isConnectorAvailable()) {
			System.out.println("OIDC Agent not found!");
			return;
		}
		else {
			System.out.println("OIDC agent at "+System.getenv(OIDC_SOCK));
		}
		OIDCAgentProxy ap = new OIDCAgentProxy();
		JSONObject j = new JSONObject();
		j.put("request", "access_token");
		j.put("account", "hbp");
		System.out.println("reply: "+ap.send(j.toString()));
	}

}
