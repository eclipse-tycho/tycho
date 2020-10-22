/*******************************************************************************
 * Copyright (c) 2019 Lablicate GmbH and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
