package eu.unicore.ucc.authn;

import java.util.HashSet;
import java.util.Set;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.USERestApplication;
import jakarta.ws.rs.core.Application;

/**
 * REST app for the notification receiver
 * 
 * @author schuller
 */
public class MockOIDCServices extends Application implements USERestApplication {

	@Override
	public void initialize(Kernel kernel) throws Exception {}

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>>classes=new HashSet<>();
		classes.add(MockOIDCServer.class);
		return classes;
	}

	public static boolean isEnabled(Kernel kernel){
		return kernel.getService("oidc")!=null;
	}
	
}
