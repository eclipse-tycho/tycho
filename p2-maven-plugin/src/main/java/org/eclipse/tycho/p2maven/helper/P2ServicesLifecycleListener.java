package org.eclipse.tycho.p2maven.helper;

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.IRepositoryIdManager;
import org.osgi.framework.Constants;

/**
 * This registers maven components as {@link IAgentServiceFactory}s inside the
 * OSGi framework
 */
@Component(role = EquinoxLifecycleListener.class, hint = "P2Services")
public class P2ServicesLifecycleListener implements EquinoxLifecycleListener {

	@Requirement
	IRepositoryIdManager repositoryIdManager;

	@Override
	public void afterFrameworkStarted(EmbeddedEquinox framework) {
		registerAgentFactory(repositoryIdManager, IRepositoryIdManager.SERVICE_NAME, framework);
	}

	private void registerAgentFactory(Object service, String serviceName, EmbeddedEquinox framework) {
		framework.registerService(IAgentServiceFactory.class, new IAgentServiceFactory() {

			@Override
			public Object createService(IProvisioningAgent agent) {
				return service;
			}
		}, Map.of(Constants.SERVICE_RANKING, 100, IAgentServiceFactory.PROP_CREATED_SERVICE_NAME, serviceName));
	}

}
