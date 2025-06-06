UCC Changelog
=============

Report any issues via the
[GitHub issue tracker](https://github.com/UNICORE-EU/commandline-client/issues)

**JAVA VERSION NOTE** This release requires Java 11 or later!

Version 10.2.1 (released MMM dd, 2025)
--------------------------------------
 - improvement: mkdir, stat, rm commands accept multiple targets

Version 10.2.0 (released May 28, 2025)
--------------------------------------
 - improvement: URL completion for storages in 'ucc shell' now includes
   job directories
 - improvement: 'list-transfers -l' uses table format 
 - fix: 'list-storages -l' did not show free space
 - fixes in example files and Windows ucc.bat script
 - code cleanup
 - update to 10.2.0 base libs


Version  10.1.4 (released Apr 07, 2025)
---------------------------------------
 - fix: OIDC server authentication should not send scope if not
   configured in preferences
 - fix: OIDC server authentication did not query for password and
   OTP value
 - fix: shell history did not work after entering password interactively

Version 10.1.3 (released Mar 18, 2025)
--------------------------------------
 - improvement: show asserted user ID (if any) in 'issue-token --inspect'
 - fix: 'list-jobs' did not work
 - fix: 'create-storage' did not work without explicit factory URL
 - update to 10.1.3 base libs

Version 10.1.2 (released Nov 22, 2024)
--------------------------------------
 - new feature: 'run' in async mode has --wait-for option to define exit point
 - improvement: 'job-status -l' shows the error message for FAILED jobs
 - update to UNICORE 10.1.2 base libs

Version 10.1.1 (released Oct 2, 2024)
-------------------------------------
 - new feature: 'job-status' can now be used to wait for a specific job status (e.g. 'RUNNING')
 - new feature: add authentication via ssh-style keys as in uftp client
 - fix: 'shell': correctly handle command help ('-h')
 - improvement: 'system-info' shows the user's available groups for core endpoints
 - fix: 'open-tunnel' failed when endpoint (gateway address) does not have explicit port
 - fix: timeouts in 'create-tss' were unrealistically short - increased to 10s
 - fix: 'run': make sure working directory is ready before uploading local files

Version 10.1.0 (released Aug 12, 2024)
--------------------------------------
 - new feature: allow to read auth token from file; allow to set
   token-type to something else than "Bearer"
 - 'rest': add '-i' option for printing the response headers
 - 'run': print job status message in case of job failure
 - 'run': new option "-J", "--multi-threaded", for running multiple jobs
   in parallel
 - improvment: oidc-agent authentication: add refreshInterval config parameter
 - fix: oidc-agent authentication: add missing dependency
 - fix: oidc-agent authentication: don't fail on missing refresh_token
 - fix: oidc-server authentication: allow to configure "oidc.scope" parameter,
   which defaults to "openid"
 - update to UNICORE 10.1 base libs

Version 10.0.0 (released Feb 22, 2024)
--------------------------------------
 - new feature: 'allocate' command for creating an allocation
 - 'shell': much more powerful commandline completion including UNICORE URLs,
   site names, and more
 - 'list-jobs': improved job list when using the "--long" option
 - 'issue-token': show subject when inspecting the token
 - 'exec': new option "--allocation" for running command in an existing allocation
 - 'shell': fix: OAuth authentication fails when refresh token is present, but expired
 - update to UNICORE 10.0.0 base libs

Version 9.3.0 (released Sept 15, 2023)
--------------------------------------
 - fix: metadata: implement missing 'search' and 'start-extract' subcommands
 - fix: wildcard download (and 'cat') did not check if the wildcard matches
 - update to UNICORE 9.3.0 base libs

Version 9.2.3 (released Jul 27, 2023)
-------------------------------------
 - new feature: OIDC-SERVER authentication method now supports 2FA / OTP and refresh token mechanism (tested with Keycloak)

Version 9.2.2 (released Jun 1, 2023)
------------------------------------
 - update to UNICORE 9.2.2 base libs
 - fix: system-info now correctly displays non-string-valued resources
 - fix: properly show errors when submitting faulty workflow

Version 9.2.0 (released Mar 21, 2023)
-------------------------------------
 - new feature: 'issue-token' command for the new endpoint '/rest/core/token', allowing to issue a JWT token
 - support non-UNICORE source/target in server-to-server copy
 - 'exec': allow to request login node, add options for async processing and setting tags
 - 'rest': allow to set Content-Type and Accept headers for GET and PUT operations

Version 9.1.0 (released Dec 13, 2022)
------------------------------------
 - update to UNICORE 9.1 base libs
 - new feature: 'open-tunnel' command for using the new port forwarding feature in UNICORE 9.1.0
 - fix: "--dry-run" option was missing for "run" command

Version 9.0.0 (released Oct 13, 2022)
------------------------------------

 - **Incompatible change:** the long forms of the commandline options have been harmonized 
   to all lowercase with "-" as separator, e.g. "--dryRun" is now "--dry-run"
 - update to UNICORE 9.0 base libs
 - new feature: "run" command supports submitting into an allocation via "--allocation" parameter
 - new feature: support UNICORE/X-internal workflow engine(s) when submitting and listing workflows
 - harmonize commandline options
 - fix: uploading local files sets their remote write/execute permission
 - version number reported via "--version" and in this change log now equal the package version
 - code cleanup

Version 2.3.0 (released Dec 15, 2021)
-------------------------------------
 - update to UNICORE 8.3 base libs
 - improvement: "run": paths for local files to upload are resolved relative to .u job file
 - improvement: "run": in async mode, write job ID file, but add "--quiet" option to allow switching this off
 - improvement: "run" and "workflow-submit" can now add tags on submission with "--tags tag1,tag2,..." option
 - fix: UFTP upload/download used obsolete single-file mode
 - fix: UFTP parameters not set
 - only allow a single preferred protocol (option -P, --protocol)
 - fix: setting user preference for group (-Z group:foo) did not have any effect
 - fix: "cp": use the transfer protocol specified in the remote URL
 - remove obsolete 'list-attributes' command
 - update bash completion

Version 2.2.1  (released Sep 27, 2021)
-------------------------------------
 - improvement: 'rest' command accepts a list of URLs
 - fix: Jline bug: clearing the screen during password entry results in characters displayed in clear text

Version 2.2.0  (released Aug 04, 2021)
-------------------------------------
 - update to UNICORE 8.2 base libs
 - fix: local imports were left in the job description, leading to job runtime erros
 - fix: UCC returns exit code 1 if job failed

Version 2.1.0  (released Feb 25, 2021)
-------------------------------------
 - update to UNICORE 8.1 base libs
 - fix: '-K', '--acceptAllIssuers' option did not work as advertised
 - fix: debian package now depends on "default-jre-headless"
 - fix: setting "uid" preference with "-Z" had no effect
 - new feature: "$_" variable in shell mode containing the last URL that was accessed / created.
 - don't write job id/properties files (less clutter)

Version 2.0.2  (released Sep 10, 2020)
-------------------------------------
 - updated workflow support to match workflow server release
 - shell: add '!' as an alias for 'system'
 - shell: improve command completion
 - fix: workflow list did not take tags into account

Version 2.0.1  (released May 19, 2020)
-------------------------------------
 - add workflow submission and management
 - default authentication method is now username/password
 - remove UNICORE 7 SAML authentication which is no longer used with the REST API
 - fix: submitted job did not wait for client stage-in 
 
Version 2.0.0  (released Mar 17, 2020)
--------------------------------------
 - update to UNICORE 8 base libs
 - commands ported to use the REST API
 - new feature: expand variables in ucc shell commands
 - new feature: "system" command in "ucc shell" allows to run external programs
 - update oidc-agent support to 3.x
 - fix: handle "=" in admin command parameter values
 - drop support for OGSA-BES
 - remove SAML-Push support
 - workflow system support temporarily not included in UCC (can use "ucc rest" if required)

Version 1.7.13 (released Apr 04, 2019)
--------------------------------------
 - fix: update to work with Java 11

Version 1.7.12 (released Sep 17, 2018)
--------------------------------------
 - new feature: support for 'oidc-agent'
 - new feature: "cp": byte range support and recursive dir copy
 - fix: attempt to directly lookup server DNs when not found in the registry
 - remove obsolete get-file, put-file, copy-file

Version 1.7.11 (released Nov 15, 2017)
---------------------------------------
 - new feature: resume mode for "cp" (client/server copy)
 - query user whether to accept unknown certificates
 - fix: protocol dependent settings ignored for server-server copy
 - show error info for failed workflows

Version 1.7.10 (released April 11, 2017)
----------------------------------------
 - remove "cip-query" command

Version 1.7.9 (released Oct 5, 2016)
------------------------------------
 - document admin commands
 - list-storages accepts list of storage URLs
 - fix: run: if "--sitename" is given, filter out non-matching sites as early as possible
 - fix: WSRF command should work without delegation 
 - fix: show compute budget in list-sites
 - default config uses truststore directory

Version 1.7.8 (released Dec 14, 2015)
-------------------------------------
 - new feature: support workflow template with parameter values read from the .u file
 - new feature: "rename" command for renaming remote files
 - new feature: --dryRun option for workflow submission
 - new feature: --raw option for 'system-info' showing registry content
 - fix: create-sms hangs when specified site or type does not exist
 - fix: local import files uploaded multiple times

Version 1.7.7 (released July 16, 2015)
--------------------------------------
 - improvement: interactively ask for myproxy username if not given
 - improvement: 'job-status' can now show more details and the job's log using the new "-l" and "-a" flags
 - fix: allow creating sweep jobs based on Arguments, Parameters, Environment and Imports tags
 - fix: Environment can now alternatively be specified in more intuitive NAME: "value" syntax
 - fix: print any error info for file up/download
 - fix: copy the default keystore to newly created ~/.ucc on windows

Version 1.7.6 (released March 13, 2015)
---------------------------------------
 - new feature: support for resource sharing via service ACLs
 - new feature: allow to set "read-only" flag on imports
 - fix: download-config can also configure Unity address and Unity authentication mode
 - fix: contact-registry flag did not work correctly with UCC BES commands
 - fix: "cp" would not copy workflow files ("c9m:...")

Version 1.7.5 (released December 19, 2014)
------------------------------------------
 - new feature: "exec" command (SF feature #347)
 - new feature: can use OIDC bearer token for authentication (authenticationMethod 'unity')
 - new feature: add way to read parameters for 'create-storage' and 'create-tss' from file
   For example: 'ucc create-storage ... @s3.properties'
 - improvement: create-storage: add "-i" option to only show info about available storage factories

Version 1.7.4 (released September 12, 2014)
-------------------------------------------
 - fix: parse error in jobs containing '#'
 - fix: issue-delegation with Unity
 - fix: "-D" option with Unity

Version 1.7.3 (released July 18, 2014)
--------------------------------------
 - new feature: "cp" command for copying files
 - new feature: persistent history of "shell" sessions
 - fix: sitename in job description was ignored
 - fix: server-to-server copy ignored preferred protocol

Version 1.7.2 (released June 13, 2014)
--------------------------------------
 - fix: when calling UCC with no arguments, show help instead of exception
 - improvement: allow comments (via '#') in .u JSON files and fix line numbers when reporting JSON errors
 - fix: specifiying trust delegation ('-D' option) had no effect
 - fix: connect-to-testgrid on Windows

Version 1.7.1 (released Feb 26, 2014)
-------------------------------------
 - new feature: allow to choose whether user pre/post command is run on login node or on compute node
 - fix: nullpointer exception when using Unity
 - fix: job and groovy script samples were out of date
 - fix: using Unity did not work
 - fix: allow unescaped "!" characters when entering a password on the console 
 - fix: 'help-auth' did not work in shell mode
 - improvement: more readable help-auth output (SF bug #688)
 - improvement: documentation on using Unity

Version 1.7.0 (released Dec 20, 2013)
-------------------------------------
 - new feature: using UNICORE 7 base libraries
 - new feature: sweep support for arguments and stage-ins
 - new feature: possibility to use the Service Orchestrator broker service (ucc run --broker SERVORCH)
 - new feature: "ucc run --dryRun ..." to only list possible resources but not submit a job
 - new feature: new "job" command for getting status, aborting, and restarting a job
 - new feature: option to get short lived certificate from myproxy
 - new feature: introduced "bes-get-output" command for fetching output files of bes activities  
 - improvement: better commandline support in shell mode
 - improvement: better performance for list-* operatons
 - improvement: unified commands "job-status", "job-abort" and "job-restart"
 - fix: in 'shell' mode UCC asks for password(s) for every command
 - fix: BES commands ignore '-s' option when contact-registry flag is set to "true"
 - fix: out-of-memory when large truststores are used
 - fix: incomplete documentation
 - fix: smarter handling of "contact-registry" flag
 - fix: "broker-run" command fails when resolving "u6://" URLs
 - fix: "broker-run" handles local files correctly and prints job address also in async mode
 - fix: security preferences (-Z option) were ignored
 - fix: "Preferred protocols" in .u file and preferences is ignored for data staging

Version 1.6.0 (released Mar 25, 2013)
-------------------------------------
 - updated to UNICORE 6.6.0 base libraries
 - full support for the EMI common authentication library (CaNL)
 - improvement: shell: better command line completion and 'unset' command to clear a variable
 - new feature: "cat" command for listing a remote file
 - new feature: pluggable authentication mechanisms
 - UFTP does no longer try to 'guess' the correct client IP

Version 1.5.1 (released Dec 3, 2012)
------------------------------------

 - fix: using resource reservation led to an error
 - improvement: "issue-delegation" does not contact registry if not necessary
 - fix: copy-file "-R" does not need an argument
 - new feature: "reservation" command to create and manage resource reservations
 - improvement: run, get-status, get-output accept multiple arguments
 - fix: add "samples" dir to deb/rpm
 - fix: missing local import files can cause errors in "get-status" and "get-outcome"

Version 1.5.0 (released May 21, 2012)
-------------------------------------
 - updated to UNICORE 6.5.0 base libraries
 - new feature: SAML push support
 - improvement: support for EMI registry
 - improvement: usage information  ("ucc <cmd> --help") reformatted
   and grouped to make it easier to read
 - new "list-transfers" command to list server-server transfers, which is
   supported by 6.5.0 and later servers
 - improvement: removed unnecessary libraries
 - improvement: more useful workflow-info output (show number of jobs, 
   storage URL, tracer URL, list job URLs)
 - improvement: accept common OS names ignoring case
 - improvement: do *not* fallback to demo registry if no registry is given
 - improvement: allow to skip registry connect using a preferences entry "contact-registry=false"
 - fix: missing local files are detected before workflow submission
 - improvement: better sample script "killall.groovy"
 - fix: wildcard exports did not work
 - improvement: when downloading results, create sub-dir for output files 
   (stdout etc) instead of using job-id as prefix
 - fix: re-try failed imports/exports once using the BFT protocol.
   If import still fails, destroy the job to prevent it staying in READY state
 - copy-file: support new reliable server-server file transfer mode
 - new feature: RunTest command for running system tests

Version 1.4.2 (released Oct 20, 2011)
-------------------------------------
 - updated to UNICORE 6.4.2 base libraries
 - improvement: display BES Factory in system-info command
 - new feature: Add new file operations setacl, chmod, chgrp, stat
 - fix: parameters from UCC properties file are properly used
 - improvement: allow to override UFTP server host with value from UCC properties
 - improvement: show progress information in data movement operations
 - improvement: better semantics for get-file and put-file when source/target is a directory
 - fix: error reporting in copy-file
 - fix: use user-defined names for stdout/stderr also for exported files
 - improvement: list-sites: "-l" gives list of logins and groups, "-s" option allows to limit list to a single site
 - suppport for specifying Unix group and (accounting) project in job .u file via "Group" and "Project" tags
 - fix: workflow: validation flag "true"/"false" was reversed
 - metadata: full storage URL can be used
 - new feature: put-file/get-file of whole directories
 - new feature: ucc-admin module for accessing the AdminService of a UNICORE/X container
 - improvement: OGSA-BES: show full job status in verbose mode
 - improvement: allow to specify ftp/scp credentials in .u file

Version 1.4.1 (released Jul 11, 2011)
-------------------------------------
 - updated to UNICORE 6.4.1 base libraries
 - new feature: support for UFTP
 - fix: metadata option "-m" is ignored
 - fix: in shell mode, exceptions during command processing do not lead to exiting the UCC
 - fix: use trust delegation for all storage commands
 - fix: in shell mode, re-configure security properties for each new command
 - improvement: add append option ("-a") in get-file
 - improvement: add helper to guess missing file transfer parameters
 - fix: check whether protocols are invalid
 - documentation update
 - new feature: in shell command, 'set' can be used to view and set properties
 - more readable details output for list-sites and list-storages
 - new WSRF operation to set the termination time of a resource
 
Version 1.4.0 (released Apr 16, 2011)
-------------------------------------

 - Java 1.6 is mandatory
 - update to UNICORE 6.4.0 base libraries
 - fix: "registry" option consumes too many command line arguments
 - fix: remove unused "site" option from list-sites
 - improvement: allow to specify job lifetime in .u file 
 - improvement: allow to set system variables (-D, -X, etc) by setting them in an environment variable UCC_OPTS
 - DEB/RPM: change installation paths to ".../unicore/ucc"
 - new feature: wildcards for local file import/export
 - support extended parameters for file transfers
 - documentation converted to Aciidoc format
 - improve data upload for workflow system: if available, use of a storage factory is preferred over shared SMS
 - add "metadata" command for accessing UNICORE's new metadata service
 - fix: file transfer protocol preferences are used consistently
 - improvement: available UCC commands are discovered via META-INF/services mechanism
 - improvement: documentation in single page HTML and in PDF format

Version 1.3.1 (released Jul 15, 2010)
-------------------------------------

 - allow to configure extra out handlers
 - more detailed output from "system-info" command
 - workflow commands can properly deal with StorageFactory
 - add Linux packages in RPM and DEB format
 - fail job if local import fails; optionally ignore this failure using a new attribute 'FailOnError'
 - allow to use EPRs for OGSA-BES factories
 - allow absolute paths in local imports/exports
 - validate the given JSDL for workflows and single jobs

Version 1.3.0 (released Feb 8, 2010)
------------------------------------

 - support for new StorageFactory service
 - new "create-storage" command
 - new "connect-to-testgrid" command for gaining access to the public testgrid at http://www.unicore.eu/testgrid
 - add "ucc shell" commandline completion for filenames
 - add "mkdir" and "rm" commands
 - fix trust delegation in "copy-file"
 - split code into multiple modules
 - commands to access OGSA-BES services included into the distribution
 - allow to issue a TD assertion using just a site name
 - fix batch mode to exit when not connected to any sites
 - fix non-working "brief" option to "Run" command
 - allow to specify both total cpus and nodes+cpus per node.
 - support specifying remote login via a user assertion (option '-U')
 - consistent non-zero exit code in case of errors

Version 1.2.2 (released Oct 13, 2009)
-------------------------------------

 - depends on UNICORE 6.2.2 libraries
 - bugfix in batch mode (would finish before jobs were all processed)
 - new "list-storages" command
 - provide commandline editing in shell mode
 - add bash completion (in "extras" directory)
 - always write job descriptor in batch "submit only" mode
 
Version 1.2.1 (released Aug 28, 2009)
-------------------------------------

 - batch mode: flag "-X" now means: do not download stdout/err, but download exports defined in .u file
 - batch mode: always print statistics on exit
 - batch mode: in case of job, failure write job descriptor (.u file) to output directory (with prefix "FAILED_"
 - consistent syntax for imports/exports and data staging
 - resolve addresses like "u6://STORAGE-NAME/..." where STORAGE-NAME is a shared SMS
 - connect command: do not print "access denied" if not in verbose mode
 - print line numbers when reporting errors in .u files
 - include the core workflow commands into the UCC base. The command names now are
   "workflow-submit", "broker-run", "system-info", "workflow-control" and "workflow-trace"
 - improve performance of connect, list-sites, run, etc commands by parallelizing the resource lookup
 - timing mode (option "-y") works uniformly for all commands
 - updated Groovy lib to 1.6.4 
 
Version 1.2.0 (released Mar 25, 2009)
-------------------------------------

 - avoid exception printout in case a site is not accessible
 - do not test the Registry connection in the 'WSRF' command 
   (the user might not want to talk to a registry)
 - Emacs mode: add command to remove job
 - simple interactive mode ("ucc shell")
 - allow to set "verbose" and "timing" mode in preferences file
 - new command "issue-delegation" to issue a trust delegation assertion
 - allow to configure default preferences location 
  (using "-Ducc.preferences=..." in the start scripts)
 - new "find" command (uses server side find new in U6.2.0)
 - support for more than one registry
 - fix finding the installation directory in the 'ucc' shell script
 - commented example user preferences file
 - improved error printout: root error is always printed, but full stack trace only in verbose mode
 - mask password entry on stdin
 - improve weighted site selection in batch mode
 - fix Windows ucc.bat to work in directories with spaces
 - added possibility to query the CIP (CIS InfoProvider) ("ucc query-cip")

Version 1.1.3 (released Oct 28, 2008)
-------------------------------------

 - check more resource requirements (e.g. operating system)
 - allow to redirect stdin, stdout and stderr in jobs
 - support JSDL creation flag (overwrite/append/nooverwrite) in stage in/out
 - allow to set site name in job description ("Site: sitename")
 - new "run" option "-H" that prints an example job and quits 
 - print the job log if job fails
 - use log4j, configured by default in conf/logging.properties
 - fix use of non-UNICORE 6 URLs in stage in/out (e.g. plain http or ftp URLs)
 - many new batch mode features and bug fixes (thanks to Richard Grunzke)
    - "submit-only" flag (-S) for batch mode
    - limit on number of new job submissions in batch mode (-M)
    - more fault-tolerant behaviour
    - pluggable site selection
    - .job file names contain the name of the request (.u) file
 - support for custom security handlers
 - improved help output (ucc -h)
 
Version 1.1.2 (released Aug 1, 2008)
------------------------------------

 - allow setting low-level options (e.g. connection timeouts)
 - fix batch mode problems (files not deleted) under Windows
 - update to unicorex 1.1.2
 
Version 1.1.1 (released May 15, 2008)
-------------------------------------

 - allow to configure separate truststore
 - add Emacs mode files in the extras/emacs-mode folder
 - batch mode accepts only ".u", ".jsdl" or ".xml" files (fix SF bug #1938686)
 - support JSDL files in batch mode
 - cache results of registry queries in batch mode 
 - cleanup of filetransfer resources
 - minor bugfixes
 
 
Version 1.1 (released Mar 20, 2008)
-----------------------------------

 - support for new UNICORE 6.1 features:
 	- setting the user name
 	- job progess indication (if available)
 - check if application is supported on TSS
 - find storage server identity for trust delegation in file transfers	
 - the 'ls -l' command now shows file modification times
 - bugfixes

Version 1.0.1
-------------


 - resolve u6:// style URLs also for stage-out
 - bug fixes
 - add ucc.bat startfile for Windows
 - new commands: 'ls' for listing a remote storage, 'abort-job'
 - new filtering option for list-jobs and list-sites commands 


Version 1.0 (released Aug 13, 2007)
-----------------------------------

 - first release.
