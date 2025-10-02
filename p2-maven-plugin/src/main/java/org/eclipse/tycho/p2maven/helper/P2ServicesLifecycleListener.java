package org.eclipse.tycho.p2maven.helper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.osgi.framework.Constants;

/**
 * This registers maven components as {@link IAgentServiceFactory}s inside the
 * OSGi framework
 */
@Named("P2Services")
@Singleton
public class P2ServicesLifecycleListener implements EquinoxLifecycleListener {

	@Inject
	Map<String, IAgentServiceFactory> agentFactories;

	@Override
	public void afterFrameworkStarted(EmbeddedEquinox framework) {
		for (var factory : agentFactories.entrySet()) {
			registerAgentFactory(factory.getKey(), factory.getValue(), framework);
		}
	}

	private void registerAgentFactory(String serviceName, IAgentServiceFactory factory, EmbeddedEquinox framework) {
		framework.registerService(IAgentServiceFactory.class, factory,
				Map.of(Constants.SERVICE_RANKING, 100, IAgentServiceFactory.PROP_CREATED_SERVICE_NAME, serviceName));
	}

}
