package eu.unicore.ucc.actions;


import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.Option;

import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.ForwardingHelper;

/**
 * Open a listening socket on the client side
 * Wait for a connection, and connect to a backend-service 
 * (port forwarding)
 *
 * @author schuller
 */
public class OpenTunnel extends ActionBase {

	private static final String OPT_LOCAL_ADDRESS_LONG = "local-address";
	private static final String OPT_LOCAL_ADDRESS = "L";

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
				.get());
		getOptions().addOption(Option.builder(OPT_KEEP)
				.longOpt(OPT_KEEP_LONG)
				.desc("Keep the local listening port open (default: true). If 'false', only one client is accepted.")
				.required(false)
				.get());
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
		return "open a port forwarding session";
	}

	@Override
	protected boolean requireRegistry(){
		return false;
	}

	@Override
	protected boolean skipConnectingToRegistry() {
		return true;
	}

	final AtomicInteger numClients = new AtomicInteger(0);

	int maxConnections = 0;

	@Override
	public void process() throws Exception {
		super.process();
		int length=getCommandLine().getArgs().length;
		if(length<2){
			throw new IllegalArgumentException("You must provide a URL as argument to this command.");
		}
		String[] localAddress = getOption(OPT_LOCAL_ADDRESS_LONG, OPT_LOCAL_ADDRESS).split(":",2);
		localPort = Integer.valueOf(localAddress[localAddress.length-1]);
		localInterface = localAddress.length>1? localAddress[0] : "localhost";
		endpoint = getCommandLine().getArgs()[1];	
		keepListening = getCommandLine().hasOption(OPT_KEEP) ?
				getBooleanOption(OPT_KEEP_LONG, OPT_KEEP) : true;
		if(!keepListening)maxConnections=1;
		doProcess();
	}

	private void doProcess() throws Exception {
		BaseClient bc = makeClient(endpoint);
		ForwardingHelper fh = new ForwardingHelper(bc);
		console.verbose("Opening listening socket on <{}:{}>, waiting for local client connection...",
				localInterface, localPort);
		try(ServerSocketChannel sc = ServerSocketChannel.open()){
			sc.configureBlocking(false);
			sc.bind(new InetSocketAddress(localInterface, localPort), 1);
			fh.accept(sc, (client)->{
				if(maxConnections>0 && numClients.get()>maxConnections) {
					throw new RuntimeException("Max number <"+maxConnections+"> of connections reached.");
				}
				numClients.incrementAndGet();
				try {
					console.verbose("Client application {} connected, connecting to backend <{}>...",
							client.getRemoteAddress(), endpoint);
					SocketChannel serviceProxy = fh.connect(endpoint);
					console.verbose("Connected, starting data forwarding.");
					fh.startForwarding(client, serviceProxy);
				}catch(Exception ex) {
					throw new RuntimeException(ex);
				}
			});
			if(localPort==0) {
				localPort = ((InetSocketAddress)sc.getLocalAddress()).getPort();
				console.info("Listening on <{}:{}>", localInterface, localPort);
			}
			fh.run();
		}
	}

	private BaseClient makeClient(String url) throws Exception {
		return new BaseClient(url,
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_UTILITY;
	}
}
