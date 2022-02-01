/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - Bug 564363 - Make ReactorProject available in MavenContext
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenContextImpl;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;

@Component(role = EquinoxLifecycleListener.class, hint = "MavenContextConfigurator")
public class MavenContextConfigurator extends EquinoxLifecycleListener {

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport context;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        MavenSession session = context.getSession();
        File localRepoRoot = new File(session.getLocalRepository().getBasedir());
        MavenLoggerAdapter mavenLogger = new MavenLoggerAdapter(logger, false);
        Properties globalProps = getGlobalProperties(session);
        MavenContextImpl mavenContext = new MavenContextImpl(localRepoRoot, session.isOffline(), mavenLogger,
                globalProps) {

            @Override
            public String getExtension(String artifactType) {
                if (artifactType == null) {
                    return "jar";
                }
                ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(artifactType);
                return handler.getExtension();
            }

        };
        for (MavenProject project : session.getProjects()) {
            mavenContext.addProject(DefaultReactorProject.adapt(project));
        }
        framework.registerService(MavenContext.class, mavenContext);
    }

    private Properties getGlobalProperties(MavenSession session) {
        Properties globalProps = new Properties();
        // 1. system
        globalProps.putAll(session.getSystemProperties());
        Settings settings = session.getSettings();
        // 2. active profiles
        Map<String, Profile> profileMap = settings.getProfilesAsMap();
        for (String profileId : settings.getActiveProfiles()) {
            Profile profile = profileMap.get(profileId);
            if (profile != null) {
                globalProps.putAll(profile.getProperties());
            }
        }
        // 3. user
        globalProps.putAll(session.getUserProperties());
        return globalProps;
    }
}
