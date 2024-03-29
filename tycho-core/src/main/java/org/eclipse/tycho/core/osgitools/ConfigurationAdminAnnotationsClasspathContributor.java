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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.osgi.framework.VersionRange;

@Component(role = ClasspathContributor.class, hint = "cm-annotations")
@SessionScoped
public class ConfigurationAdminAnnotationsClasspathContributor extends AbstractSpecificationClasspathContributor {

    private static final String PACKAGE_NAME = "org.osgi.service.cm.annotations";
    private static final String GROUP_ID = "org.osgi";
    //TODO there is no extra artifact see https://github.com/osgi/osgi/issues/567
    private static final String ARTIFACT_ID = "org.osgi.service.cm";
    private static final VersionRange VERSION = new VersionRange("[1.5,2)");

    @Inject
    protected ConfigurationAdminAnnotationsClasspathContributor(MavenSession session) {
        super(session, PACKAGE_NAME, GROUP_ID, ARTIFACT_ID);
    }

    @Override
    protected VersionRange getSpecificationVersion(MavenProject project) {
        return VERSION;
    }

}
