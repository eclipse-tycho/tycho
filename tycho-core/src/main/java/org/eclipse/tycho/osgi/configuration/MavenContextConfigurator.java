/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;

@Component(role = EquinoxLifecycleListener.class, hint = "MavenContextConfigurator")
public class MavenContextConfigurator extends EquinoxLifecycleListener {

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport context;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        MavenSession session = context.getSession();
        File localRepoRoot = new File(session.getLocalRepository().getBasedir());
        MavenLoggerAdapter mavenLogger = new MavenLoggerAdapter(logger, false);
        Properties globalProps = getGlobalProperties(session);
        MavenContext mavenContext = new MavenContextImpl(localRepoRoot, session.isOffline(), mavenLogger, globalProps);
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
            globalProps.putAll(profileMap.get(profileId).getProperties());
        }
        // 3. user
        globalProps.putAll(session.getUserProperties());
        return globalProps;
    }
}
