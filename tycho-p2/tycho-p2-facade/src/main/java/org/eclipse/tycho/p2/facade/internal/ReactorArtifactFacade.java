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
package org.eclipse.tycho.p2.facade.internal;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;

public class ReactorArtifactFacade implements IReactorArtifactFacade {
    private final ReactorProject wrappedProject;

    private final String classifier;

    public ReactorArtifactFacade(ReactorProject otherProject, String classifier) {
        this.wrappedProject = otherProject;
        this.classifier = classifier;
    }

    public File getLocation() {
        return wrappedProject.getBasedir();
    }

    public String getGroupId() {
        return wrappedProject.getGroupId();
    }

    public String getArtifactId() {
        return wrappedProject.getArtifactId();
    }

    public String getClassifier() {
        return classifier;
    }

    public String getVersion() {
        return wrappedProject.getVersion();
    }

    public String getPackagingType() {
        return wrappedProject.getPackaging();
    }

    public Set<Object/* IInstallableUnit */> getDependencyMetadata(boolean primary) {
        Set<Object> result = wrappedProject.getDependencyMetadata(classifier, primary);
        return result != null ? result : Collections.emptySet();
    }

    public String getClassidier() {
        return classifier;
    }
}
