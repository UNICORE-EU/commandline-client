package de.fzj.unicore.ucc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.ucc.helpers.JLineLogger;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.data.UFTPConstants;
import eu.unicore.ucc.actions.data.LS;
import eu.unicore.ucc.actions.shell.Shell;
import eu.unicore.ucc.io.FiletransferParameterProvider;

public class TestVarious {

	@Test
	public void test1(){
		LS ls=new LS();
		FileListEntry f= new FileListEntry("foo", new JSONObject());
		f.isDirectory = true;
		f.size = 1234;
		Calendar c=Calendar.getInstance();
		f.lastAccessed = ls.format(c);
		f.permissions = "--x";
		String reply=ls.detailedListing(f);
		System.out.println(reply);
		assertTrue(reply.contains("foo"));
		assertTrue(reply.contains("1234"));
		assertTrue(reply.contains("d--x"));
		assertTrue(reply.contains(ls.format(c)));
	}

	@Test
	public void test3()throws Exception{
		System.out.println(getFullyQualifiedHostName());
	}

	@Test
	public void testUFTPParameterProvider(){
		Map<String,String>params = new HashMap<String, String>();
		new FiletransferParameterProvider().provideParameters(params, "UFTP");
		Assert.assertEquals("1", params.get(UFTPConstants.PARAM_STREAMS));
		Assert.assertNotNull(params.get(UFTPConstants.PARAM_SECRET));
	}
	

	private static String getFullyQualifiedHostName()
	{
		try {
			String canonical = "";
			Enumeration<NetworkInterface> enet = NetworkInterface.getNetworkInterfaces();
			List<String> names = new ArrayList<String>();
			while ( enet.hasMoreElements())
			{
				NetworkInterface net = enet.nextElement();
				if ( net.isLoopback() )
					continue;

				Enumeration<InetAddress> eaddr = net.getInetAddresses();
				while ( eaddr.hasMoreElements() )
				{
					InetAddress inet = eaddr.nextElement();

					if ( inet.getCanonicalHostName().equalsIgnoreCase( inet.getHostAddress() ) )
						continue;
					else
					{
						names.add(inet.getCanonicalHostName());
					}
				}
			}


			canonical = InetAddress.getLocalHost().getCanonicalHostName();
			if(canonical != null)
			{
				for(String name : names)
				{
					if(name.startsWith(canonical)) return name; 
				}
				return names.isEmpty() ? canonical : names.get(0); 
			}
			else
			{
				return names.isEmpty() ? "localhost" : names.get(0); 
			}

		} catch (Exception e) {
			return "localhost";
		}

	}
	
	@Test
	public void testShellArgsParsing() throws Exception {
		String cmd = "this is a test";
		List<String> res = Arrays.asList(Shell.parseCmdline(cmd));
		assertEquals(4, res.size());
		
		cmd = "this \"is a\" test";
		res = Arrays.asList(Shell.parseCmdline(cmd));
		assertEquals(3, res.size());
		assertEquals("is a", res.get(1));
		
		cmd = "this \"is a test\"";
		res = Arrays.asList(Shell.parseCmdline(cmd));
		assertEquals(2, res.size());
		assertEquals("is a test", res.get(1));
		
		
		cmd = "set a=b";
		res = Arrays.asList(Shell.parseCmdline(cmd));
		assertEquals(2, res.size());
		assertEquals("a=b", res.get(1));
		
	}
	
	@Test
	public void testJLineLogging() throws Exception {
		JLineLogger.init();
		org.jline.utils.Log.info("test123");
	}

	@Test
	public void testArgParsing() throws Exception {
		Options options = new Options();
		options.addOption(Option.builder("B")
				.longOpt("bytes")
				.desc("Byte range")
				.argName("ByteRange")
				.hasArg()
				.required(false)
				.build());
		CommandLineParser p = new DefaultParser();
		CommandLine cl = p.parse(options, new String [] {"-B", "0-9"});
		assertEquals("0-9", cl.getOptionValue("B"));
	}
	
}
