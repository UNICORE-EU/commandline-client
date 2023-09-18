package eu.unicore.ucc.util;

import javax.inject.Inject;

import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.io.ChangeACL;
import de.fzj.unicore.xnjs.tsi.local.LocalTS;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIProperties;

public class MockLocalTSI extends LocalTS {

	@Inject
	public MockLocalTSI(InternalManager manager, LocalTSIProperties properties, XNJSProperties xnjsProps){
		super(manager, properties, xnjsProps);
	}
	
	@Override
	public void chgrp(String file, String newGroup, boolean recursive)
			throws ExecutionException {
		// NOP
	}
	
	@Override
	public boolean isACLSupported(String path) {
		return true;
	}

	@Override
	public void setfacl(String file, boolean clearAll, ChangeACL[] changeACL, boolean recursive)
			throws ExecutionException {
		// NOP
	}

}
