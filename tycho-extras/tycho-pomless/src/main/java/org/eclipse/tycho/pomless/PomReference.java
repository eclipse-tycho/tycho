/*******************************************************************************
 * Copyright (c) 2019 Lablicate GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;

import org.apache.maven.model.io.ModelReader;

/**
 * Reference to a pom and the corresponding reader
 *
 */
public class PomReference {

    private File pom;
    private ModelReader reader;

    public PomReference(File pom, ModelReader reader) {
        this.pom = pom;
        this.reader = reader;
    }

    public File getPomFile() {
        return pom;
    }

    public ModelReader getReader() {
        return reader;
    }

}
