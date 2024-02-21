package eu.unicore.ucc.actions.shell;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.UCCConfigurationProvider;

/**
 * Completer for UNICORE URLs in the shell command line
 *
 * @author schuller
 */
public class URLCompleter {

	private final UCCConfigurationProvider configurationProvider;
	
	public URLCompleter(UCCConfigurationProvider configurationProvider){
		this.configurationProvider = configurationProvider;
	}

	final List<String> endpoints = Arrays.asList( "core/", "registries/", "workflows/", "admin", 
			"auth/", "share/" );

	final List<String> services = Arrays.asList( "factories/", "sites/", "jobs/", "reservations/",
			"storages/", "storagefactories/", "transfers/", "client-server-transfers/", "tasks/" );

	static final Set<String> sites = new HashSet<>();

	public static void registerSiteURL(String siteURL) {
		if(siteURL!=null && siteURL.contains("/rest/")) {
			sites.add(siteURL.substring(0, siteURL.indexOf("/rest/")+6));
		}
	}

	/**
	 * check if the current thing is a UNICORE URL and try to complete it
	 *
	 * @return true if we found something and the completion process should stop
	 */
	public boolean completeURLs(LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
		try{
			String current = line.word();
			if(!current.startsWith("https:"))
				return false;
			if(!current.contains("/rest/"))
				return tryCompleteSite(reader, line, candidates);
			if(tryCompleteEndpoint(reader, line, candidates))
				return true;
			if(tryCompleteService(reader, line, candidates))
				return true;
			URL u = new URL(current);
			String base = current.substring(0, current.lastIndexOf("/")+1);
			String fragment = current.substring(current.lastIndexOf("/")+1);
			String dir = "";
			boolean fileMode = false;
			if(base.contains("/files/") && base.contains("/rest/core/storages/")) {
				fragment = u.getPath().split("/files/", 2)[1];
				while(fragment.startsWith("/"))fragment=fragment.substring(1);
				base = current.replace(fragment, "");
				// fragment may be a directory - split once more
				if(fragment.indexOf("/")>1){
					dir = fragment.substring(0, fragment.lastIndexOf("/")+1);
					fragment = fragment.substring(fragment.lastIndexOf("/")+1);
					base = base+dir;
				}
				fileMode = true;
			}
			var bsc = new BaseServiceClient(new Endpoint(base),
					configurationProvider.getClientConfiguration(current),
					configurationProvider.getRESTAuthN());
			var j = bsc.getProperties();
			var js = j.toString();
			if(fileMode) {
				JSONObject files = j.getJSONObject("content");
				for(String fileName: files.keySet()) {
					JSONObject info = files.getJSONObject(fileName);
					if(fileName.startsWith("/"))fileName=fileName.substring(1);
					if(fileName.startsWith(fragment)) {
						boolean isFile = !info.getBoolean("isDirectory");
						String value = base+fileName.replaceFirst(dir, "");
						candidates.add(new Candidate(value, value, null, null, null, null, isFile, 0));
					}
				}
				return true;
			}
			else {
				var p = Pattern.compile("\""+base+fragment+"[^\"]*\"");
				Matcher m = p.matcher(js);
				boolean found = false;
				while(m.find()) {
					String v = m.group();
					v = v.substring(1, v.length()-1);
					if(!v.endsWith("/"))v=v+"/";
					candidates.add(new Candidate(v, v, null, null, null, null, false, 0));
					found = true;
				}
				if(found) {
					return true;
				}
			}
		} catch(Exception e) {
			if(UCC.unitTesting)System.out.println("ERROR: "+e);
		}
		return false;
	}
	
	private boolean tryCompleteSite(LineReader reader, ParsedLine line, List<Candidate> candidates) throws Exception {
		doComplete(sites, "", "", reader, line, candidates);
		return true;
	}
	
	private boolean tryCompleteEndpoint(LineReader reader, ParsedLine line, List<Candidate> candidates) throws Exception {
		String current = line.word();
		URL u = new URL(current);
		String fragment = u.getPath().split("/rest/", 2)[1];
		if(fragment.indexOf("/")==-1) {
			String base = current.substring(0, current.lastIndexOf("/")+1);
			doComplete(endpoints, base, fragment, reader, line, candidates);
			return true;
		}
		return false;
	}

	private boolean tryCompleteService(LineReader reader, ParsedLine line, List<Candidate> candidates) throws Exception {
		String current = line.word();
		URL u = new URL(current);
		String[] ts = u.getPath().split("/rest/core/", 2);
		if(ts.length<2)return false;
		String fragment = ts[1];
		if(fragment.indexOf("/")==-1) {
			String base = current.substring(0, current.lastIndexOf("/")+1);
			doComplete(services, base, fragment, reader, line, candidates);
			return true;
		}
		return false;
	}

	private void doComplete(final Collection<String> completions, final String base, final String fragment, 
			final LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
		for(String c: completions) {
			if(c.startsWith(fragment)) {
				String v = base+c;
				candidates.add(new Candidate(v, v, null, null, null, null, false, 0));
			}
		}
	}
}