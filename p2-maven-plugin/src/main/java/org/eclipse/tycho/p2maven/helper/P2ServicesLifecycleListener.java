package org.eclipse.tycho.p2maven.helper;

import java.util.Map;

import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * This registers maven components as {@link IAgentServiceFactory}s inside the
 * OSGi framework
 */
@Singleton
@Named("P2Services")
public class P2ServicesLifecycleListener implements EquinoxLifecycleListener {

	private final Map<String, IAgentServiceFactory> agentFactories;

	@Inject
	public P2ServicesLifecycleListener(Map<String, IAgentServiceFactory> agentFactories) {
		this.agentFactories = agentFactories;
	}

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
