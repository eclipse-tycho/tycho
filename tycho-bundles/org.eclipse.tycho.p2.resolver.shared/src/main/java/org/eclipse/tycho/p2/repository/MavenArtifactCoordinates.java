/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.repository;

// TODO revise/document
public class MavenArtifactCoordinates {

    private final GAV gav;
    private final String classifier;
    private final String extension;

    public MavenArtifactCoordinates(GAV gav, String classifier, String extension) {
        super();
        this.gav = gav;
        this.classifier = classifier;
        this.extension = extension;
    }

    public GAV getGav() {
        return gav;
    }

    public String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    public String getVersion() {
        return gav.getVersion();
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

}
