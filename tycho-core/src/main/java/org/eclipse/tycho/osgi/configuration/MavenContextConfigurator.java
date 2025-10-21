/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - Bug 564363 - Make ReactorProject available in MavenContext
 *                          Issue #797 - Implement a caching P2 transport  
 *                          Issue #829 - Support maven --strict-checksums option
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.shared.MavenContext;

@Named("MavenContextConfigurator")
@Singleton
public class MavenContextConfigurator implements EquinoxLifecycleListener {

    @Inject
    private MavenContext context;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        //we call all methods here to init the data for sure inside the maven thread...
        context.getChecksumsMode();
        context.isUpdateSnapshots();
        context.getSessionProperties();
        context.getLocalRepositoryRoot();
        context.getMavenRepositoryLocations();
        context.getProjects();
        context.isOffline();
        framework.registerService(MavenContext.class, context);
    }

    static Properties getGlobalProperties(MavenSession session) {
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
