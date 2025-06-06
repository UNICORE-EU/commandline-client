package eu.unicore.ucc.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.Option;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.security.OperationType;
import eu.unicore.uas.json.JSONUtil;

/**
 * Shares resources and shows resource ACLs
 *
 * @author schuller
 */
public class Share extends ActionBase {

	public static final String OPT_CLEAN_LONG = "clean";
	public static final String OPT_CLEAN = "b";
	public static final String OPT_DELETE_LONG = "delete";
	public static final String OPT_DELETE = "d";

	@Override
	protected void createOptions() {
		super.createOptions();

		getOptions().addOption(Option.builder(OPT_CLEAN)
				.longOpt(OPT_CLEAN_LONG)
				.desc("Prior to applying all other ACEs (if any are present) the ACL of the resource is cleared.")
				.required(false)
				.build());
		getOptions().addOption(Option.builder(OPT_DELETE)
				.longOpt(OPT_DELETE_LONG)
				.desc("Specified ACEs are deleted from the resource's ACL (if this option is not specified then ACEs are added).")
				.required(false)
				.build());
	}

	@Override
	public String getName() {
		return "share";
	}

	@Override
	public String getSynopsis() {
		return "Shares a remote resource by modifying its ACL. By default the command will add " +
				"the specified Access Control Entries (ACEs) to the resource's ACL. With the '"
				+ OPT_DELETE_LONG + "' option the ACEs are removed. The ACEs specification is " +
				"required always unless the '" + OPT_CLEAN_LONG + "' option is specified. " +
				"The ACE syntax is as follows:\n\n" +
				"[read|modify]:[DN|VO|GROUP|UID|ROLE]:[value]\n" +
				"If no ACEs are given, the current ACL is shown.";
	}

	@Override
	public String getDescription(){
		return "share a resource via its ACL";
	}

	@Override
	public String getArgumentList(){
		return "[ACE[ ACE...]] <URL>";
	}

	@Override
	public String getCommandGroup(){
		return CMD_GRP_OTHER;
	}

	@Override
	public void process() throws Exception {
		super.process();
		boolean clean = getBooleanOption(OPT_CLEAN_LONG, OPT_CLEAN);
		console.verbose("Remove all ACL entries = {}", clean);
		boolean delete = getBooleanOption(OPT_DELETE_LONG, OPT_DELETE);
		console.verbose("Delete given ACL entries = {}", delete);

		int length=getCommandLine().getArgs().length;
		boolean onlyShow = !clean && length<3;
		if(length<2)
		{
			console.error(null, "Not enough arguments!");
			printUsage();
			return;
		}
		//URL is last argument
		String url=getCommandLine().getArgs()[length-1];
		Endpoint epr = new Endpoint(url);
		console.verbose("Modifying ACLs of: {}", url);
		BaseServiceClient client = new BaseServiceClient(epr,
				configurationProvider.getClientConfiguration(url),
				configurationProvider.getRESTAuthN());

		List<ACLEntry> permits = new ArrayList<>();
		for(String acl: JSONUtil.toList(client.getProperties().getJSONArray("acl"))) {
			permits.add(ACLEntry.parse(acl));
		}
		lastNumberOfPermits = permits.size();

		if(onlyShow){
			console.info("Current ACL for {}", url);
			for(ACLEntry p: permits){
				console.info("- allow '{}' when '{}' is '{}'", p.accessType, p.matchType, p.requiredValue);
			}
		}
		else {
			if(clean){
				permits.clear();
			}
			int deleted = 0;
			for(int i = 1;i<length-1;i++){
				ACLEntry p = ACLEntry.parse(getCommandLine().getArgs()[i]);
				if(delete){
					Iterator<ACLEntry>iter = permits.iterator();
					while(iter.hasNext()){
						ACLEntry existing = iter.next();
						if(existing.toString().equals(p.toString())){
							iter.remove();
							deleted++;
							break;
						}
					}
				}
				else{
					permits.add(p);
				}
			}
			for(ACLEntry p: permits){
				console.verbose("-> allow '{}' when '{}' is '{}'", p.accessType, p.matchType, p.requiredValue);
			}
			JSONObject newProps = new JSONObject();
			JSONArray arr = new JSONArray();
			for(ACLEntry e: permits) {
				arr.put(e.toString());
			}
			newProps.put("acl", arr);
			JSONObject res = client.setProperties(newProps);
			try{
				console.info("Service reply: {}", res.getString("acl"));
				if(delete){
					console.info("<{}> ACL entries were deleted.", deleted);
				}
			}catch(Exception ex) {}
		}
	}

	private static final List<String> allowed = Arrays.asList(new String[]{"DN","VO","GROUP","UID","ROLE"});

	public static class ACLEntry {

		public static enum MatchType {
			DN, VO, GROUP, UID, ROLE;
		}

		// the operation type allowed by this entry
		final OperationType accessType;

		// the required value
		final String requiredValue;

		// the ACL entry type
		final MatchType matchType;

		public ACLEntry(OperationType grant, String clientAttribute, MatchType ofType){
			if(grant==null||clientAttribute==null||ofType==null){
				throw new IllegalArgumentException("Parameter(s) cannot be null");
			}
			this.accessType = grant;
			this.requiredValue = clientAttribute;
			this.matchType = ofType;
		}

		public String toString(){
			return accessType+":"+matchType+":"+requiredValue;
		}

		/**
		 * parse the given string which is expected to be in the form 
		 * "accessType:matchType:requiredValue" and provide proper errors
		 *  
		 * @param acl - string representation of an ACLEntry
		 */
		public static ACLEntry parse(String acl){
			String [] tokens = null;
			OperationType op = null;
			MatchType match = null;
			try{
				tokens = acl.split(":", 3);
			}catch(Exception ex){
				throw new IllegalArgumentException("Wrong format, expecting 'accessType:matchType:requiredValue'", ex);
			}
			try{
				op = OperationType.valueOf(tokens[0]);
			}catch(Exception ex) {
				throw new IllegalArgumentException("Wrong access type, must be 'read', 'write' or 'modify'", ex);
			}
			try{
				match = MatchType.valueOf(tokens[1]);
			}catch(Exception ex) {
				throw new IllegalArgumentException("Wrong access type, must be one of "+allowed, ex);
			}

			return new ACLEntry(op, tokens[2], match);
		}

	}

	// unit testing
	public static int lastNumberOfPermits=0;
}





