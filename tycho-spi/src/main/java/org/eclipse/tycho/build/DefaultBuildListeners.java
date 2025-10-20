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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.helper.PluginRealmHelper;

@Named
public class DefaultBuildListeners implements BuildListeners {
    @Inject
    private PluginRealmHelper realmHelper;

    @Inject
    private Logger log;

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
                if (log.isDebugEnabled()) {
                    log.error(message, e);
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
                if (log.isDebugEnabled()) {
                    log.error(message, e);
                }
            }
        }

    }
}
