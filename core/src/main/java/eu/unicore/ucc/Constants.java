package eu.unicore.ucc;

public interface Constants {

	//exit codes
	
	//general error
	public static final int ERROR=1;
	
	//some client-side error
	public static final int ERROR_CLIENT=2;
	
	//some server-side error
	public static final int ERROR_SERVER=3;
	
	//connections and networking
	public static final int ERROR_CONNECTION=4;
	
	//validation error
	public static final int ERROR_JOB_INVALID=5;
	
	//requested resource not found
	public static final int NO_SUCH_RESOURCE=404;
		
	//security errors
	public static final int ERROR_SECURITY=128;
	
	public static final int ERROR_CERT_INVALID=129;
	
	public static final String OPT_PROPERTIES_LONG="configuration";
	public static final String OPT_PROPERTIES="c";
	
	public static final String OPT_OUTPUT_LONG="output";
	public static final String OPT_OUTPUT="o";
	
	public static final String OPT_REGISTRY_LONG="registry";
	public static final String OPT_REGISTRY="r";
	
	public static final String OPT_VERBOSE_LONG="verbose";
	public static final String OPT_VERBOSE="v";
	
	public static final String OPT_HELP_LONG="help";
	public static final String OPT_HELP="h";
	
	public static final String OPT_TIMING_LONG="with-timing";
	public static final String OPT_TIMING="y";

	public static final String OPT_SITENAME_LONG="sitename";
	public static final String OPT_SITENAME="s";

	public static final String OPT_LOGIN_NODE_LONG="login-node";
	public static final String OPT_LOGIN_NODE="L";

	public static final String OPT_NOPREFIX_LONG="brief";
	public static final String OPT_NOPREFIX="b";

	public static final String OPT_SAMPLE_LONG="example";
	public static final String OPT_SAMPLE="H";
	
	public static final String OPT_DETAILED_LONG="long";
	public static final String OPT_DETAILED="l";

	public static final String OPT_SHOW_META_LONG="show-metadata";
	public static final String OPT_SHOW_META="m";

	public static final String OPT_ALL_LONG="all";
	public static final String OPT_ALL="a";

	public static final String OPT_RAW_LONG = "raw";
	public static final String OPT_RAW = "R";
	
	public static final String OPT_FIELDS_LONG="fields";
	public static final String OPT_FIELDS="F";

	public static final String OPT_TAGS_LONG="tags";
	public static final String OPT_TAGS="T";
	
	public static final String OPT_FILTER_LONG="filter";
	public static final String OPT_FILTER="f";
	
	public static final String OPT_MODE_LONG="asynchronous";
	public static final String OPT_MODE="a";
	
	public static final String OPT_ALLOCATION_LONG="allocation";
	public static final String OPT_ALLOCATION="A";
	
	public static final String OPT_GROOVYSCRIPT_LONG="file";
	public static final String OPT_GROOVYSCRIPT="f";
	
	public static final String OPT_GROOVYEXPRESSION_LONG="expression";
	public static final String OPT_GROOVYEXPRESSION="e";
	
	public static final String OPT_FOLLOW_LONG="follow";
	public static final String OPT_FOLLOW="f";
	
	public static final String OPT_INPUTDIR_LONG="input";
	public static final String OPT_INPUTDIR="i";
	
	public static final String OPT_KEEP_LONG="keep";
	public static final String OPT_KEEP="Q";
	
	public static final String OPT_MAXRUNNING_LONG="max";
	public static final String OPT_MAXRUNNING="m";
	
	public static final String OPT_MAXREQUESTS_LONG="max-new-jobs";
	public static final String OPT_MAXREQUESTS="M";
	
	public static final String OPT_UPDATEINTERVAL_LONG="update";
	public static final String OPT_UPDATEINTERVAL="u";
	
	public static final String OPT_NUMTHREADS_LONG="threads";
	public static final String OPT_NUMTHREADS="t";
	
	public static final String OPT_NOCHECKRESOURCES_LONG="no-resource-check";
	public static final String OPT_NOCHECKRESOURCES="R";
	
	public static final String OPT_NOFETCHOUTCOME_LONG="no-fetch-outcome";
	public static final String OPT_NOFETCHOUTCOME="X";
	
	public static final String OPT_LIFETIME_LONG="lifetime";
	public static final String OPT_LIFETIME="l";
	
	public static final String OPT_RECURSIVE_LONG="recursive";
	public static final String OPT_RECURSIVE="R";

	public static final String OPT_HUMAN_LONG="human";
	public static final String OPT_HUMAN="H";

	public static final String OPT_WEIGHTS_LONG="site-weights";
	public static final String OPT_WEIGHTS="W";

	public static final String OPT_SUBMIT_ONLY_LONG="submit-only";
	public static final String OPT_SUBMIT_ONLY="S";

	public static final String OPT_FORCE_REMOTE_MODE_LONG="force-remote";
	public static final String OPT_FORCE_REMOTE_MODE="f";
	
	public static final String OPT_FACTORY_LONG="factory-url";
	public static final String OPT_FACTORY="f";

	public static final String OPT_SCHEDULED_LONG="schedule";
	public static final String OPT_SCHEDULED="S";

	public static final String OPT_BROKER_LONG="broker";
	public static final String OPT_BROKER="B";
	
	public static final String OPT_DRYRUN_LONG="dry-run";
	public static final String OPT_DRYRUN="d";
	
	public static final String OPT_QUIET_LONG="quiet";
	public static final String OPT_QUIET="q";
	
	public static final String OPT_SECURITY_PREFERENCES="Z";
	public static final String OPT_SECURITY_PREFERENCES_LONG="preference";
	
	public static final String OPT_AUTHN_METHOD="k";
	public static final String OPT_AUTHN_METHOD_LONG="authentication-method";
	
	public static final String OPT_AUTHN_ACCEPT_ALL="K";
	public static final String OPT_AUTHN_ACCEPT_ALL_LONG="accept-all-issuers";
	
	// key for the session ID file
	public static final String SESSION_ID_FILEKEY = "ucc-session-ids";
	
	// properties key for the "last useful output" generated by various commands
	public static final String PROP_LAST_RESOURCE_URL = "_";

	// Command group names
	public static final String CMD_GRP_UTILITY = "Utilities";
	public static final String CMD_GRP_JOBS = "Job execution";
	public static final String CMD_GRP_DATA = "Data management";
	public static final String CMD_GRP_ADMIN = "Administration";
	public static final String CMD_GRP_WORKFLOW = "Workflow";
	public static final String CMD_GRP_OTHER = "Other";
	
}
