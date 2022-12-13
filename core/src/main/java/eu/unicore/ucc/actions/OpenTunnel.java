package eu.unicore.ucc.actions;


import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.commons.cli.Option;

import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.ForwardingHelper;

/**
 * Open a listening socket on the client side
 * Wait for a connection, and connect to a backend-service 
 * (port forwarding)
 * 
 * @author schuller
 */
public class OpenTunnel extends ActionBase {

	public static final String OPT_LOCAL_ADDRESS_LONG = "local-address";
	public static final String OPT_LOCAL_ADDRESS = "L";


	private String endpoint;

	private int localPort;

	private String localInterface;

	private boolean keepListening;

	@Override
	protected void createOptions() {
		super.createOptions();
		getOptions().addOption(Option.builder(OPT_LOCAL_ADDRESS)
				.longOpt(OPT_LOCAL_ADDRESS_LONG)
				.desc("Local address <[interface:]port> to listen on (port=0 to let system choose)")
				.hasArg()
				.required(true)
				.build());
		getOptions().addOption(Option.builder(OPT_KEEP)
				.longOpt(OPT_KEEP_LONG)
				.desc("Keep the local port open after client application disconnects")
				.required(false)
				.build());
	}
	@Override
	public String getName(){
		return "open-tunnel";
	}

	@Override
	public String getArgumentList(){
		return "URL";
	}

	@Override
	public String getSynopsis(){
		return "Starts a port forwarding session. Opens a listening socket, " + 
				"waits for a connection, and connects this new connection to the "
				+ "backend-service given by the URL argument.";
	}

	@Override
	public String getDescription(){
		return "Open a port forwarding session";
	}

	protected boolean requireRegistry(){
		return false;
	}

	protected boolean skipConnectingToRegistry() {
		return true;
	}

	@Override
	public void process(){
		super.process();
		try{
			int length=getCommandLine().getArgs().length;
			if(length<2){
				throw new IllegalArgumentException("You must provide a URL as argument to this command.");
			}
			String[] localAddress = getOption(OPT_LOCAL_ADDRESS_LONG, OPT_LOCAL_ADDRESS).split(":",2);
			localPort = Integer.valueOf(localAddress[localAddress.length-1]);
			localInterface = localAddress.length>1? localAddress[0] : "localhost";
			endpoint = getCommandLine().getArgs()[1];	
			keepListening = getBooleanOption(OPT_KEEP_LONG, OPT_KEEP);
			doProcess();
		}catch(Exception e){
			error("Can't open tunnel", e);
			endProcessing(ERROR);
		}
	}

	protected void doProcess() throws Exception {
		verbose("Opening listening socket on <"+localInterface+":"+localPort+">");
		SocketChannel client = null;
		ServerSocketChannel sc = null;
		try{
			sc = ServerSocketChannel.open();
			sc.bind(new InetSocketAddress(localInterface, localPort), 1);
			if(localPort==0) {
				localPort = ((InetSocketAddress)sc.getLocalAddress()).getPort();
				message("Listening on <"+localInterface+":"+localPort+">");
			}
			do {
				verbose("Waiting for client to connect.");
				client = sc.accept();
				verbose("Client application connected, connecting to backend <"+endpoint+"> ...");
				BaseClient bc = makeClient(endpoint);
				ForwardingHelper fh = new ForwardingHelper(bc);
				SocketChannel serviceProxy = fh.connect(endpoint);
				verbose("Connected, starting data forwarding.");
				fh.startForwarding(client, serviceProxy);
				verbose("Disconnected.");
			}while(keepListening);
		}
		finally {
			sc.close();
		}
	}

	protected BaseClient makeClient(String url) throws Exception {
		return new BaseClient(url,
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
	}

}
