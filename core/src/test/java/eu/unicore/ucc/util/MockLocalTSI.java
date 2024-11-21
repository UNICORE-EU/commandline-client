package eu.unicore.ucc.util;

import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.io.ChangeACL;
import eu.unicore.xnjs.tsi.local.LocalTS;
import eu.unicore.xnjs.tsi.local.LocalTSIProperties;
import jakarta.inject.Inject;

public class MockLocalTSI extends LocalTS {

	@Inject
	public MockLocalTSI(InternalManager manager, LocalTSIProperties properties, XNJSProperties xnjsProperties){
		super(manager, properties, xnjsProperties);
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
