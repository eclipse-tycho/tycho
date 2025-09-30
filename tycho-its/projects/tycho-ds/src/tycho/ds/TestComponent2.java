package tycho.ds;

import javax.inject.Singleton;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Singleton
public class TestComponent2 {
	
	@Reference
	private Runnable service;
	
	@Activate
	public void activate(BundleContext bundleContext) {
		
	}

}
