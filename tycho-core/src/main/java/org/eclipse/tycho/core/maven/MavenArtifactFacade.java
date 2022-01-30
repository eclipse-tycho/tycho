/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Christoph Läubrich - add toString/equals/hashCode
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public final class MavenArtifactFacade implements IArtifactFacade {

    private final Artifact mavenArtifact;
    private String repositoryId;

    public MavenArtifactFacade(Artifact mavenArtifact) {
        this.mavenArtifact = mavenArtifact;
        ArtifactRepository repository = mavenArtifact.getRepository();
        if (repository != null) {
            repositoryId = repository.getId();
        } else {
            File file = mavenArtifact.getFile();
            if (file != null) {
                File repositoriesFile = new File(file.getParentFile(), "_remote.repositories");
                if (repositoriesFile.isFile()) {
                    Properties properties = new Properties();
                    try {
                        properties.load(new FileInputStream(repositoriesFile));
                        for (String name : properties.stringPropertyNames()) {
                            String[] split = name.split(">", 2);
                            if (split.length == 2) {
                                if (split[0].equals(file.getName())) {
                                    repositoryId = split[1];
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        //can't find the repository id then!
                    }
                }
            }
        }
    }

    @Override
    public String getRepository() {
        return repositoryId;
    }

    @Override
    public File getLocation() {
        return mavenArtifact.getFile();
    }

    @Override
    public String getGroupId() {
        return mavenArtifact.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return mavenArtifact.getArtifactId();
    }

    @Override
    public String getVersion() {
        // bug 352154: getVersion has expanded/non-expanded SNAPSHOT, depending on if the artifact is cached or available from remote 
        return mavenArtifact.getBaseVersion();
    }

    @Override
    public String getPackagingType() {
        return mavenArtifact.getType();
    }

    @Override
    public String getClassifier() {
        return mavenArtifact.getClassifier();
    }

    @Override
    public List<String> getDependencyTrail() {
        List<String> trail = mavenArtifact.getDependencyTrail();
        if (trail == null || trail.isEmpty()) {
            return Collections.singletonList(mavenArtifact.getId());
        }
        return trail;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mavenArtifact);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MavenArtifactFacade other = (MavenArtifactFacade) obj;
        return Objects.equals(mavenArtifact, other.mavenArtifact);
    }

    @Override
    public String toString() {
        return "MavenArtifactFacade [wrappedArtifact=" + mavenArtifact + "]";
    }

}
