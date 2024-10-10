/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.build;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.helper.PluginRealmHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class BuildListeners {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PluginRealmHelper realmHelper;

    @Inject
    public BuildListeners(PluginRealmHelper realmHelper) {
        this.realmHelper = realmHelper;
    }

    public void notifyBuildStart(MavenSession session) {
        Set<String> called = new HashSet<String>();
        for (MavenProject project : session.getProjects()) {
            try {
                realmHelper.visitPluginExtensions(project, session, BuildListener.class, listener -> {
                    if (called.add(listener.getClass().getName())) {
                        listener.buildStarted(session);
                    }
                });
            } catch (Exception e) {
                String message = "Can't call BuildListeners for project: " + project.getId();
                if (logger.isDebugEnabled()) {
                    logger.error(message, e);
                }
            }
        }
    }

    public void notifyBuildEnd(MavenSession session) {
        Set<String> called = new HashSet<String>();
        for (MavenProject project : session.getProjects()) {
            try {
                realmHelper.visitPluginExtensions(project, session, BuildListener.class, listener -> {
                    if (called.add(listener.getClass().getName())) {
                        listener.buildEnded(session);
                    }
                });
            } catch (Exception e) {
                String message = "Can't call BuildListeners for project: " + project.getId();
                if (logger.isDebugEnabled()) {
                    logger.error(message, e);
                }
            }
        }

    }
}
