package org.eclipse.tycho.core.maven.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginDescriptorCache;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Reflection helper which handles incompatible changes in maven core between maven 3.0.x and maven
 * 3.1
 */
@Component(role = MavenCompatiblityHelper.class)
public class MavenCompatiblityHelper {
    @Requirement
    private Logger logger;

    @Requirement
    protected MavenPluginManager mavenPluginManager;
    private Method getPluginDescriptorMethod;

    @Requirement
    private PluginDescriptorCache pluginDescriptorCache;
    private Method createKeyMethod;

    private Method getRepositorySessionMethod;

    public MavenCompatiblityHelper() {
        getPluginDescriptorMethod = getMethod(MavenPluginManager.class, "getPluginDescriptor");
        createKeyMethod = getMethod(PluginDescriptorCache.class, "createKey");
        getRepositorySessionMethod = getMethod(MavenSession.class, "getRepositorySession");
    }

    @SuppressWarnings("rawtypes")
    private static Method getMethod(Class clazz, String methodName) {
        for (Method method : clazz.getMethods()) {
            if (methodName.equals(method.getName())) {
                return method;
            }
        }
        throw new RuntimeException("Method '" + methodName + "' not found for class " + clazz.getName());
    }

    /**
     * Equivalent to {@link MavenPluginManager#getPluginDescriptor(Plugin,
     * project.getRemotePluginRepositories(), session.getRepositorySession())}.
     * 
     * The types RemoteRepository and RepositorySystemSession from aether are changed incompatibly
     * in maven 3.1 so we invoke MavenPluginManager#getPluginDescriptor reflectively. See maven
     * issue <a href="http://jira.codehaus.org/browse/MNG-5354">MNG-5354</a>.
     * 
     */
    public PluginDescriptor getPluginDescriptor(Plugin plugin, MavenProject project, MavenSession session)
            throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException {
        try {
            Object remoteRepositories = project.getRemotePluginRepositories();
            Object repositorySession = getRepositorySessionMethod.invoke(session);
            return (PluginDescriptor) getPluginDescriptorMethod.invoke(mavenPluginManager, plugin, remoteRepositories,
                    repositorySession);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }

    /**
     * Equivalent to {@link PluginDescriptorCache#createKey(Plugin,
     * project.getRemotePluginRepositories(), session.getRepositorySession())}.
     * 
     * The types RemoteRepository and RepositorySystemSession from aether are changed incompatibly
     * in maven 3.1 so we invoke PluginDescriptorCache#createKey reflectively. See maven issue <a
     * href="http://jira.codehaus.org/browse/MNG-5354">MNG-5354</a>.
     */
    public PluginDescriptorCache.Key createKey(Plugin plugin, MavenProject project, MavenSession session) {
        try {
            Object repositorySession = getRepositorySessionMethod.invoke(session);
            Object remoteRepositories = project.getRemotePluginRepositories();

            return (PluginDescriptorCache.Key) createKeyMethod.invoke(pluginDescriptorCache, plugin,
                    remoteRepositories, repositorySession);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            throw new RuntimeException(e);
        }
    }

}
