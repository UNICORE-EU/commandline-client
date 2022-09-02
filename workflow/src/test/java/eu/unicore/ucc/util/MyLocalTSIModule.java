package eu.unicore.ucc.util;

import java.util.Properties;

import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.ems.LocalECManager;
import de.fzj.unicore.xnjs.idb.GrounderImpl;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.IDBImpl;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.impl.FileTransferEngine;
import de.fzj.unicore.xnjs.tsi.BasicExecution;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.IExecutionSystemInformation;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.local.LocalTS;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;

public class MyLocalTSIModule extends LocalTSIModule {

	public MyLocalTSIModule(Properties properties) {
		super(properties);
	}

	@Override
	protected void configure(){

		bind(IExecutionContextManager.class).to(LocalECManager.class);
		bind(IExecution.class).to(BasicExecution.class);
		bind(IExecutionSystemInformation.class).to(BasicExecution.class);
		bind(TSI.class).to(LocalTS.class);

		bind(Incarnation.class).to(GrounderImpl.class);
		bind(IDB.class).to(IDBImpl.class);

		bind(IFileTransferEngine.class).to(FileTransferEngine.class);
		bind(IReservation.class).to(MockReservation.class);
	}
}
