/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.util.Objects;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.p2maven.advices.MavenPropertiesAdvice;

public final class AetherArtifactFacade implements IArtifactFacade {

    private final Artifact aetherArtifact;
    private String repositoryId;

    public AetherArtifactFacade(Artifact aetherArtifact) {
        this.aetherArtifact = aetherArtifact;
        //TODO is there a better way?
        repositoryId = MavenPropertiesAdvice.getRepository(aetherArtifact.getFile());
    }

    @Override
    public String getRepository() {
        return repositoryId;
    }

    @Override
    public File getLocation() {
        return aetherArtifact.getFile();
    }

    @Override
    public String getGroupId() {
        return aetherArtifact.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return aetherArtifact.getArtifactId();
    }

    @Override
    public String getVersion() {
        return aetherArtifact.getBaseVersion();
    }

    @Override
    public String getPackagingType() {
        return aetherArtifact.getExtension();
    }

    @Override
    public String getClassifier() {
        return aetherArtifact.getClassifier();
    }

    @Override
    public int hashCode() {
        return Objects.hash(aetherArtifact);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AetherArtifactFacade other = (AetherArtifactFacade) obj;
        return Objects.equals(aetherArtifact, other.aetherArtifact);
    }

    @Override
    public String toString() {
        return "AetherArtifactFacade [for =" + aetherArtifact + "]";
    }

    /**
     * 
     * @return the artifact this facades
     */
    public Artifact getArtifact() {
        return aetherArtifact;
    }

}
