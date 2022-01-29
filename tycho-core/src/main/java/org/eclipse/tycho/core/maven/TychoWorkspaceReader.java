/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

@Component(role = WorkspaceReader.class, hint = "TychoWorkspaceReader")
public class TychoWorkspaceReader implements WorkspaceReader {

    private WorkspaceRepository repository;

    public TychoWorkspaceReader() {
        System.out.println("Hello from TychoWorkspaceReader!");
        //TODO this requires https://issues.apache.org/jira/browse/MNG-7400
        repository = new WorkspaceRepository("tycho", null);
    }

    @Override
    public WorkspaceRepository getRepository() {
        return repository;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        if ("pom".equals(artifact.getExtension())) {
            return null;
        }
        System.out.println("TychoWorkspaceReader: findArtifact(" + artifact + ")");
        return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return Collections.emptyList();
    }

}
