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

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.osgi.framework.Version;

@Component(role = ClasspathContributor.class, hint = "bundle-annotations")
@SessionScoped
public class BundleAnnotationsClasspathContributor extends AbstractSpecificationClasspathContributor {

    private static final String PACKAGE_NAME = "org.osgi.annotation.bundle";
    private static final String GROUP_ID = "org.osgi";
    private static final String ARTIFACT_ID = "org.osgi.annotation.bundle";
    private static final Version VERSION = new Version(1, 0, 0);

    @Inject
    protected BundleAnnotationsClasspathContributor(MavenSession session) {
        super(session, PACKAGE_NAME, GROUP_ID, ARTIFACT_ID);
    }

    @Override
    protected Version getSpecificationVersion(ReactorProject project) {
        return VERSION;
    }

}
