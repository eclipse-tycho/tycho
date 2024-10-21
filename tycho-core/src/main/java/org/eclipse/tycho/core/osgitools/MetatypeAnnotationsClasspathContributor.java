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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.VersionRange;

@Singleton
@Named("metatype-annotations")
public class MetatypeAnnotationsClasspathContributor extends AbstractSpecificationClasspathContributor {

    private static final String PACKAGE_NAME = "org.osgi.service.metatype.annotations";
    private static final String GROUP_ID = "org.osgi";
    private static final String ARTIFACT_ID = "org.osgi.service.metatype.annotations";
    private static final VersionRange VERSION = new VersionRange("[1,2)");

    @Inject
    protected MetatypeAnnotationsClasspathContributor(MavenBundleResolver mavenBundleResolver, Provider<MavenSession> sessionProvider) {
        super(mavenBundleResolver, sessionProvider, PACKAGE_NAME, GROUP_ID, ARTIFACT_ID);
    }

    @Override
    protected VersionRange getSpecificationVersion(MavenProject project) {
        return VERSION;
    }

}
