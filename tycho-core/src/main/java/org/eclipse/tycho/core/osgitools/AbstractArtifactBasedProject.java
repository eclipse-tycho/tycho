/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;

public abstract class AbstractArtifactBasedProject extends AbstractTychoProject {
    // this is stricter than Artifact.SNAPSHOT_VERSION
    public static final String SNAPSHOT_VERSION = "-SNAPSHOT";

    // requires resolved target platform
    @Override
    public ArtifactDependencyWalker getDependencyWalker(MavenProject project) {
        return getDependencyWalker(project, null);
    }

    @Override
    public ArtifactDependencyWalker getDependencyWalker(MavenProject project, TargetEnvironment environment) {
        return newDependencyWalker(project, environment);
    }

    protected abstract ArtifactDependencyWalker newDependencyWalker(MavenProject project,
            TargetEnvironment environment);

    @Override
    public void checkForMissingDependencies(MavenProject project) {
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);

        // this throws exceptions when dependencies are missing
        getDependencyWalker(project).walk(new ArtifactDependencyVisitor() {
        });
    }

    protected String getOsgiVersion(ReactorProject project) {
        String version = project.getVersion();
        if (version.endsWith(SNAPSHOT_VERSION)) {
            version = version.substring(0, version.length() - SNAPSHOT_VERSION.length()) + ".qualifier";
        }
        if (version.contains("-") && !version.endsWith(SNAPSHOT_VERSION)) {
            return processVersionWithDashes(version);
        }
        return version;
    }

    private String processVersionWithDashes(String version) {
        List<String> splittedVersion = Arrays.asList(version.split("-"));
        String validOsgiVersion = "";
        if (!splittedVersion.isEmpty() && splittedVersion.size() > 1) {
            String osgiVersion = "";
            if (splittedVersion.get(0).matches("^[0-9]{1}.[0-9]{1}.[0-9]{1}$")) {
                osgiVersion = splittedVersion.get(0) + ".";
            }
            String qualifier = "";
            Iterator<String> iterator = splittedVersion.iterator();
            iterator.next();
            while (iterator.hasNext()) {
                qualifier += iterator.next() + "-";
            }
            validOsgiVersion = osgiVersion + qualifier.substring(0, qualifier.length() - 1);
        }
        return validOsgiVersion;
    }
}
