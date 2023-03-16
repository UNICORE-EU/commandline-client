package eu.unicore.ucc;

import eu.unicore.ucc.actions.Connect;
import eu.unicore.ucc.actions.Groovy;
import eu.unicore.ucc.actions.OpenTunnel;
import eu.unicore.ucc.actions.REST;
import eu.unicore.ucc.actions.Share;
import eu.unicore.ucc.actions.IssueToken;
import eu.unicore.ucc.actions.admin.AdminServiceInfo;
import eu.unicore.ucc.actions.admin.RunCommand;
import eu.unicore.ucc.actions.data.CP;
import eu.unicore.ucc.actions.data.CatFile;
import eu.unicore.ucc.actions.data.CopyFileStatus;
import eu.unicore.ucc.actions.data.CreateStorage;
import eu.unicore.ucc.actions.data.GetFileProperties;
import eu.unicore.ucc.actions.data.LS;
import eu.unicore.ucc.actions.data.Metadata;
import eu.unicore.ucc.actions.data.Mkdir;
import eu.unicore.ucc.actions.data.RM;
import eu.unicore.ucc.actions.data.Rename;
import eu.unicore.ucc.actions.data.Resolve;
import eu.unicore.ucc.actions.data.Umask;
import eu.unicore.ucc.actions.info.ListJobs;
import eu.unicore.ucc.actions.info.ListSites;
import eu.unicore.ucc.actions.info.ListStorages;
import eu.unicore.ucc.actions.info.ListTransfers;
import eu.unicore.ucc.actions.info.SystemInfo;
import eu.unicore.ucc.actions.job.AbortJob;
import eu.unicore.ucc.actions.job.Batch;
import eu.unicore.ucc.actions.job.CreateTSS;
import eu.unicore.ucc.actions.job.Exec;
import eu.unicore.ucc.actions.job.GetOutcome;
import eu.unicore.ucc.actions.job.GetStatus;
import eu.unicore.ucc.actions.job.JobRestart;
import eu.unicore.ucc.actions.job.Run;
import eu.unicore.ucc.actions.shell.Shell;

/**
 * provides the set of commands in the UCC core module
 */
public class CoreCommands implements ProvidedCommands {

	@Override
	public Command[] getCommands() {
		return new Command[]{

				// job execution
				new CreateTSS(),
				new Run(),
				new GetStatus(),
				new GetOutcome(),
				new Batch(),
				new AbortJob(),
				new JobRestart(),
				new Exec(),
				
				// data management
				new CreateStorage(),
				new CopyFileStatus(),
				new LS(),
				new Mkdir(),
				new RM(),
				new Metadata(),
				new GetFileProperties(),
				new Umask(),
				new CatFile(),
				new CP(),
				new Rename(),
				new Resolve(),
				
				// informational
				new ListSites(),
				new ListJobs(),
				new ListStorages(),
				new ListTransfers(),
				new SystemInfo(),

				// other
				new Connect(),
				new Groovy(),
				new Shell(),
				new Share(),
				new REST(),
				new OpenTunnel(),
				new IssueToken(),

				//admin
				new RunCommand(),
				new AdminServiceInfo(),
		};

	}

}
