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
package org.eclipse.tycho.versions;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.versions.engine.PomVersionChange;
import org.eclipse.tycho.versions.engine.VersionsEngine;
import org.eclipse.tycho.versions.pom.GAV;
import org.eclipse.tycho.versions.pom.PomFile;

/**
 * <p>
 * Updates the parent version in a pom file
 * </p>
 * 
 */
@Mojo(name = "set-parent-version", aggregator = true, requiresDependencyResolution = ResolutionScope.NONE)
public class SetParentVersionMojo extends AbstractChangeMojo {

    private final class ParentPomVersionChange extends PomVersionChange {
        private final GAV parent;

        private ParentPomVersionChange(PomFile pom, String version, String newVersion, GAV parent) {
            super(pom, version, newVersion);
            this.parent = parent;
        }

        @Override
        public String getGroupId() {
            return parent.getGroupId();
        }

        @Override
        public String getArtifactId() {
            return parent.getArtifactId();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + getEnclosingInstance().hashCode();
            result = prime * result + Objects.hash(parent);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            ParentPomVersionChange other = (ParentPomVersionChange) obj;
            if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                return false;
            return Objects.equals(parent, other.parent);
        }

        private SetParentVersionMojo getEnclosingInstance() {
            return SetParentVersionMojo.this;
        }
    }

    /**
     * <p>
     * The new parent version to set to the current project.
     * </p>
     */
    @Parameter(property = "newParentVersion", required = true)
    private String newParentVersion;

    @Override
    protected void addChanges(List<String> artifacts, VersionsEngine engine)
            throws MojoExecutionException, IOException {
        if (newParentVersion == null || newParentVersion.isEmpty()) {
            throw new MojoExecutionException("Missing required parameter newParentVersion");
        }
        for (String artifactId : artifacts) {
            PomFile pom = engine.getMutablePom(artifactId);
            GAV parent = pom.getParent();
            if (parent == null) {
                continue;
            }
            engine.addVersionChange(new ParentPomVersionChange(pom, parent.getVersion(), newParentVersion, parent));
        }
    }

}
