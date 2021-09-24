package de.fzj.unicore.ucc.shell;

public class TestUCCCompletor {

//	@Test
//	public void testCompleteCommand(){
//		String[]cmd={"foo","bar","ham"};
//		List<String>completions=new ArrayList<String>();
//		new UCCCompletor(cmd).complete("f", 0, completions);
//		assertEquals(1,completions.size());
//		new UCCCompletor(cmd).complete("ba", 1, completions);
//		assertEquals(1,completions.size());
//		assertEquals("bar ",completions.get(0));
//	}
//	
//	@Test
//	public void testFileNameCompletion(){
//		String[]cmd={"foo","bar","ham"};
//		String filePattern="simple";
//		List<String>completions=new ArrayList<String>();
//		new UCCCompletor(cmd).completeFileName("", new File("src/test/resources/uas/"),filePattern, 1, completions);
//		assertEquals(1,completions.size());
//		assertEquals("src/test/resources/uas/simpleidb ",completions.get(0));
//	}
//
//	@Test
//	public void testFileNameCompletionAbsolute(){
//		File base=new File("src/test/resources/uas/").getAbsoluteFile();
//		String filePattern="si";
//		String[]cmd={"foo","bar","ham"};
//		List<String>completions=new ArrayList<String>();
//		new UCCCompletor(cmd).completeFileName("", base, filePattern, 1, completions);
//		assertEquals(1,completions.size());
//		String expect=new File(base,"simpleidb").getAbsolutePath();
//		assertEquals(expect+" ",completions.get(0));
//	}
//	
//	@Test
//	public void testCompleteDirectoryNames(){
//		File base=new File("src/test/resources/");
//		String filePattern="";
//		int num=base.list().length;
//		List<String>completions=new ArrayList<String>();
//		String[]cmd={"foo","bar","ham"};
//		new UCCCompletor(cmd).completeFileName("", base, filePattern, 1, completions);
//		assertEquals(num,completions.size());
//	}
//
//	@Test
//	public void testCompleteCommandOption(){
//		UCC.cmds.put("mock", MockCommand.class);
//		String[]cmd={"mock", "foo"};
//		List<String>completions=new ArrayList<String>();
//		String buf="mock --x";
//		new UCCCompletor(cmd).complete(buf, buf.length(), completions);
//		assertEquals(1,completions.size());
//		System.out.println(completions.get(0));
//		assertTrue(completions.get(0).endsWith("--xarg "));
//	}
//	
//	public static class MockCommand extends Command{
//		
//		@SuppressWarnings("all")
//		protected void createOptions(){
//			getOptions().addOption(new Option("X","xarg",false,"testing option")
//			,UCCOptions.GRP_GENERAL);
//		}
//		
//	}

}
