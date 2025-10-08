package eu.unicore.ucc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bjoernh
 */
public class MockServer extends Thread {

	private boolean active = true;
	private final ServerSocket socket;
	private final boolean okMode;

	public MockServer(boolean okMode) throws IOException {
		this.okMode = okMode;
		socket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
	}

	public int getServerPort() {
		return socket.getLocalPort();
	}

	@Override
	public void run() {
		while(active) {
			try {
				Socket conn = socket.accept();
				System.out.println("MockServer: New connection from " + conn.getRemoteSocketAddress());
				if(okMode) {
					new OKConnection(conn).start();
				}else {
					new EchoConnection(conn).start();
				}
			} catch (IOException e) {}
		}
	}

	private class EchoConnection extends Thread {

		private final Socket conn;

		public EchoConnection(Socket _conn) {
			this.conn = _conn;
		}

		@Override
		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				OutputStream out = conn.getOutputStream();
				String line = in.readLine();
				if(line!=null) {
					System.out.println("ECHO: Echoing back: " + line);
					out.write((line+"\n").getBytes());
				}
			} catch (IOException e) {
				// we're done
			}
			System.out.println("ECHO: Closed connection with " + conn.getRemoteSocketAddress());
		}
	}

	private class OKConnection extends Thread {

		private final Socket conn;

		public OKConnection(Socket _conn) {
			this.conn = _conn;
		}

		@Override
		public void run() {
			try {
				parseHttp(conn.getInputStream());
				OutputStream out = conn.getOutputStream();
				out.write(("HTTP/1.1 204 No Content\n\n").getBytes());
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("OK: Closed connection with " + conn.getRemoteSocketAddress());
		}

		private void parseHttp(InputStream input) throws UnsupportedEncodingException{
			BufferedReader br = new BufferedReader(new InputStreamReader(input,"UTF-8"));
			try{
				String line = br.readLine(); // first line e.g. "GET / HTTP/1.1"
				List<String> _hdrs = new ArrayList<>();
				StringBuilder sb=new StringBuilder(1024);
				if (line != null) {
					line=br.readLine();
					int contentLength=0;
					while(line!= null && line.length()>0){
						_hdrs .add(line.trim());
						if(line.toLowerCase().startsWith("Content-Length:")){
							try{
								contentLength=Integer.parseInt(line.substring("Content-Length:".length()).trim());
							}
							catch(Exception ex){ /* */}
						}
						line=br.readLine();
					}
					boolean chunked =_hdrs.contains("Transfer-Encoding: chunked");
					if((!chunked && contentLength==0) || line==null){
						return;
					}
					line = br.readLine();
					if(chunked){
						big_loop: while(line!= null){
							int chunksize = 0;
							try{
								chunksize = Integer.parseInt(line, 16);
							}
							catch(Exception ex){/*bad request, bad!*/}
							if(chunksize == 0) break;
							int counter=0;
							while(true){
								line = br.readLine();
								if(line==null) break big_loop;
								counter+=line.length(); //this only works with one-byte encodings
								sb.append(line);
								if(counter<chunksize) {
									sb.append("\n");
									counter+=2;
								}
								if(counter>=chunksize){
									break;
								}
							}
							line = br.readLine();
							while(line!= null && line.length()==0){
								line = br.readLine();
							}
						}
					}
					else{
						if(contentLength>0) {
							char[]buf = new char[contentLength];
							br.read(buf);
							sb.append(new String(buf));
						}
					}
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public boolean isActive() {
		return active;
	}

	public void shutdown() {
		this.active = false;
	}
}
