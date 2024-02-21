package eu.unicore.ucc.authn;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;

/**
 * mock OIDC server
 * 
 * @author schuller
 */
public class MockOIDCServerFeature extends FeatureImpl {

	public MockOIDCServerFeature() {
		this.name = "OIDCServer";
	}

	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		services.add(new NotificationSD(kernel));	
		
	}

	public static class NotificationSD extends DeploymentDescriptorImpl {

		public NotificationSD(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public NotificationSD() {
			super();
			this.name = "oidc";
			this.type = RestService.TYPE;
			this.implementationClass = MockOIDCServices.class;
		}

	}
}
