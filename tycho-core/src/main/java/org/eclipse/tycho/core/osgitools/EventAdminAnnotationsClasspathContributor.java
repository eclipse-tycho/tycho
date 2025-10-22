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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.osgi.framework.VersionRange;

@Named("eventadmin-annotations")
@SessionScoped
public class EventAdminAnnotationsClasspathContributor extends AbstractSpecificationClasspathContributor {

    private static final String PACKAGE_NAME = "org.osgi.service.event.annotations";
    private static final String GROUP_ID = "org.osgi";
    //TODO there is no extra artifact see https://github.com/osgi/osgi/issues/567
    private static final String ARTIFACT_ID = "org.osgi.service.event";
    private static final VersionRange VERSION = new VersionRange("[1.3,2)");

    @Inject
    protected EventAdminAnnotationsClasspathContributor(MavenSession session) {
        super(session, PACKAGE_NAME, GROUP_ID, ARTIFACT_ID);
    }

    @Override
    protected VersionRange getSpecificationVersion(MavenProject project) {
        return VERSION;
    }

}
