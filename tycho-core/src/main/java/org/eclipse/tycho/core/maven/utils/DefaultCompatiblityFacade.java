package org.eclipse.tycho.core.maven.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginDescriptorCache;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.repository.RemoteRepository;

@Component(role = CompatibilityFacade.class)
public class DefaultCompatiblityFacade implements CompatibilityFacade {
    @Requirement
    private Logger logger;

    @Requirement
    protected MavenPluginManager mavenPluginManager;

    @Requirement
    private PluginDescriptorCache pluginDescriptorCache;

    private Method getPluginDescriptor;

    private Method getRepositorySession;

    private Method createKey;

    public DefaultCompatiblityFacade() {
        try {
            for (Method m : MavenPluginManager.class.getMethods()) {
                if ("getPluginDescriptor".equals(m.getName())) {
                    getPluginDescriptor = m;
                }
            }
        } catch (SecurityException e) {
            logger.warn("unable to find MavenPluginManager.setupPluginRealm() method", e);
        }

        try {
            for (Method m : MavenSession.class.getMethods()) {
                if ("getRepositorySession".equals(m.getName())) {
                    getRepositorySession = m;
                    break;
                }
            }
        } catch (SecurityException e) {
            logger.warn("unable to find MavenSession.getRepositorySession() method", e);
        }

        try {
            for (Method m : PluginDescriptorCache.class.getMethods()) {
                if ("createKey".equals(m.getName())) {
                    createKey = m;
                    break;
                }
            }
        } catch (SecurityException e) {
            logger.warn("unable to find MavenSession.getRepositorySession() method", e);
        }
    }

    public PluginDescriptor getPluginDescriptor(Plugin plugin, List<RemoteRepository> repositories, MavenSession session)
            throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException {
        try {
            Object repositorySession = getRepositorySession.invoke(session);

            return (PluginDescriptor) getPluginDescriptor.invoke(mavenPluginManager, plugin, repositories,
                    repositorySession);
        } catch (IllegalArgumentException e) {
            logger.warn("IllegalArgumentException during MavenPluginManager.getPluginDescriptor() call", e);
        } catch (IllegalAccessException e) {
            logger.warn("IllegalAccessException during MavenPluginManager.getPluginDescriptor() call", e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof PluginResolutionException) {
                throw (PluginResolutionException) target;
            }
            if (target instanceof PluginDescriptorParsingException) {
                throw (PluginDescriptorParsingException) target;
            }
            if (target instanceof InvalidPluginDescriptorException) {
                throw (InvalidPluginDescriptorException) target;
            }
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            if (target instanceof Error) {
                throw (Error) target;
            }
            logger.warn("Exception during MavenPluginManager.getPluginDescriptor() call", e);
        }

        return null;
    }

    public PluginDescriptorCache.Key createKey(Plugin plugin, List<RemoteRepository> repositories, MavenSession session) {
        try {
            Object repositorySession = getRepositorySession.invoke(session);

            return (PluginDescriptorCache.Key) createKey.invoke(pluginDescriptorCache, plugin, repositories,
                    repositorySession);
        } catch (IllegalArgumentException e) {
            logger.warn("IllegalArgumentException during MavenPluginManager.getPluginDescriptor() call", e);
        } catch (IllegalAccessException e) {
            logger.warn("IllegalAccessException during MavenPluginManager.getPluginDescriptor() call", e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            if (target instanceof Error) {
                throw (Error) target;
            }
            logger.warn("Exception during MavenPluginManager.getPluginDescriptor() call", e);
        }

        return null;
    }

}
