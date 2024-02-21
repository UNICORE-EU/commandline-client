package eu.unicore.ucc.actions.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.junit.Test;

import eu.unicore.ucc.UCC;
import eu.unicore.ucc.authn.UCCConfigurationProvider;
import eu.unicore.ucc.util.EmbeddedTestBase;

/**
 * Functional tests for the UCC actions. 
 * These run against an embedded UNICORE instance.
 */
public class TestShell extends EmbeddedTestBase {

	@Test
	public void test_Shell()throws Exception{
		connect();
		String[]args=new String[]{"shell",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-f", "src/test/resources/shell_input",
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		Shell shell = (Shell)UCC.lastCommand;
		assertNull(shell.getProperties().get("ham"));
		assertEquals("bar",shell.getProperties().get("foo"));
	}

	
	static int wordIndex = 0;
	static int wordCursor = 0;
	static int cursor = 0;
	String line = null;

	@Test
	public void test_Shell_Basic_Completer() {
		wordIndex = 0;
		wordCursor = 0;
		cursor = 0;
		line = null;
		Set<String> cmds = new HashSet<>();
		cmds.addAll(UCC.cmds.keySet());
		UCCCompleter cc = new UCCCompleter(cmds, null);
		var candidates = new ArrayList<Candidate>();
		var words = new ArrayList<String>();
		var pl = new ParsedLine() {
			
			public List<String> words() {
				return words;
			}
			
			public int wordIndex() { return wordIndex; }
			
			public int wordCursor() { return wordCursor; }
			
			public String word() { return words.get(wordIndex); }
			
			public String line() { return line; }

			public int cursor() { return cursor; }
		};

		cc.complete(null, pl, candidates);
		assertEquals(cmds.size(), candidates.size());
		
		// "run --..."
		words.clear();
		words.add("run");
		words.add("--");
		wordIndex = 1;
		wordCursor = 0;
		line = "run --";
		cursor = 6;
		candidates.clear();
		LineReader lr = LineReaderBuilder.builder().build();
		cc.complete(lr, pl, candidates);

		assertTrue(candidates.contains(new Candidate("--help")));
		assertFalse(candidates.contains(new Candidate("pom.xml")));
		
		// "run ..."
		words.clear();
		words.add("run");
		words.add("");
		wordIndex = 1;
		wordCursor = 0;
		line = "run ";
		cursor = 4;
		candidates.clear();
		cc.complete(lr, pl, candidates);

		assertTrue(candidates.contains(new Candidate("pom.xml")));

		// "rest ..."
		words.clear();
		words.add("rest");
		words.add("");
		wordIndex = 1;
		wordCursor = 0;
		line = "rest ";
		cursor = 5;
		candidates.clear();
		cc.complete(lr, pl, candidates);
		assertEquals(4, candidates.size());
		assertTrue(candidates.contains(new Candidate("DELETE")));
	}

	@Test
	public void test_Shell_URL_Completer() throws Exception {
		String[]args=new String[]{"shell",
				"-c", "src/test/resources/conf/userprefs.embedded",
				"-f",  "src/test/resources/shell_input_short"
		};
		UCC.main(args);
		assertEquals(Integer.valueOf(0),UCC.exitCode);
		Shell shell = (Shell)UCC.lastCommand;
		UCCConfigurationProvider configProvider = shell.getConfigurationProvider();
		
		wordIndex = 0;
		wordCursor = 0;
		cursor = 0;
		line = null;
		Set<String> cmds = new HashSet<>();
		cmds.addAll(UCC.cmds.keySet());
		URLCompleter uc = new URLCompleter(configProvider);
		var candidates = new ArrayList<Candidate>();
		var words = new ArrayList<String>();
		var pl = new ParsedLine() {
			
			public List<String> words() {
				return words;
			}
			
			public int wordIndex() { return wordIndex; }
			
			public int wordCursor() { return wordCursor; }
			
			public String word() { return words.get(wordIndex); }
			
			public String line() { return line; }

			public int cursor() { return cursor; }
		};

		// ".../rest/..."
		String url = "https://localhost:65322/rest/";
		words.clear();
		words.add("rest");
		words.add("get");
		words.add(url);
		
		wordIndex = 2;
		wordCursor = url.length();
		line = "rest get "+url;
		cursor = line.length();
		candidates.clear();
		LineReader lr = LineReaderBuilder.builder().build();
		assertTrue(uc.completeURLs(lr, pl, candidates));
		assertEquals(uc.endpoints.size(), candidates.size());
		assertTrue(candidates.contains(new Candidate(url+"core/")));

		
		// ".../rest/registries/..."
		url = "https://localhost:65322/rest/registries/";
		words.clear();
		words.add("rest");
		words.add("get");
		words.add(url);
		wordIndex = 2;
		wordCursor = url.length();
		line = "rest get "+url;
		cursor = line.length();
		candidates.clear();
		assertTrue(uc.completeURLs(lr, pl, candidates));
		assertEquals(2, candidates.size());
		assertTrue(candidates.contains(new Candidate(url+"default_registry/")));
		
		// ".../rest/core/..."
		url = "https://localhost:65322/rest/core/";
		words.clear();
		words.add("rest");
		words.add("get");
		words.add(url);
		wordIndex = 2;
		wordCursor = url.length();
		line = "rest get "+url;
		cursor = line.length();
		candidates.clear();
		assertTrue(uc.completeURLs(lr, pl, candidates));
		assertEquals(uc.services.size(), candidates.size());
		assertTrue(candidates.contains(new Candidate(url+"jobs/")));
		
		// ".../rest/core/storages/..."
		url = "https://localhost:65322/rest/core/storages/";
		words.clear();
		words.add("rest");
		words.add("get");
		words.add(url);
		wordIndex = 2;
		wordCursor = url.length();
		line = "rest get "+url;
		cursor = line.length();
		candidates.clear();
		assertTrue(uc.completeURLs(lr, pl, candidates));
		assertTrue(candidates.contains(new Candidate(url+"WORK/")));
		
		// ".../rest/core/storages/WO..."
		url = "https://localhost:65322/rest/core/storages/WO";
		words.clear();
		words.add("rest");
		words.add("get");
		words.add(url);
		wordIndex = 2;
		wordCursor = url.length();
		line = "rest get "+url;
		cursor = line.length();
		candidates.clear();
		assertTrue(uc.completeURLs(lr, pl, candidates));
		assertTrue(candidates.contains(new Candidate(url+"RK/")));
		
		// ".../rest/core/storages/WORK/files/..."
		url = "https://localhost:65322/rest/core/storages/WORK/files/";
		words.clear();
		words.add("rest");
		words.add("get");
		words.add(url);
		wordIndex = 2;
		wordCursor = url.length();
		line = "rest get "+url;
		cursor = line.length();
		candidates.clear();
		assertTrue(uc.completeURLs(lr, pl, candidates));
		assertTrue(candidates.size()>1);
		
		// ".../rest/core/storages/WORK/files/Task.mv..."
		url = "https://localhost:65322/rest/core/storages/WORK/files/Task.mv";
		words.clear();
		words.add("rest");
		words.add("get");
		words.add(url);
		wordIndex = 2;
		wordCursor = url.length();
		line = "rest get "+url;
		cursor = line.length();
		candidates.clear();
		assertTrue(uc.completeURLs(lr, pl, candidates));
		assertEquals(1, candidates.size());

		File tDir = new File("target/data", "test123");
		FileUtils.forceMkdir(tDir);
		FileUtils.write(new File(tDir, "test.dat"), "testdata", "UTF-8");
		
		// ".../rest/core/storages/WORK/files/test123/..."
		url = "https://localhost:65322/rest/core/storages/WORK/files/test123/";
		words.clear();
		words.add("rest");
		words.add("get");
		words.add(url);
		wordIndex = 2;
		wordCursor = url.length();
		line = "rest get "+url;
		cursor = line.length();
		candidates.clear();
		assertTrue(uc.completeURLs(lr, pl, candidates));
		assertEquals(1, candidates.size());
	}
}
