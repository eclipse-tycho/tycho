package tycho.ds;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class TestComponent {
	
	@Reference
	private Runnable service;
	
	@Activate
	public void activate(BundleContext bundleContext) {
		
	}

}
