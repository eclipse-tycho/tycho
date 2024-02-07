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
package org.eclipse.tycho.model.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * represents information gathered from an "eclipse project" usually stored in a file named
 * <code>.project</code>
 *
 */
public interface EclipseProject {

    String getName();

    String getComment();

    Path getLocation();

    boolean hasNature(String nature);

    static EclipseProject parse(Path projectFile) throws IOException {
        return ProjectParser.parse(projectFile);
    }

    /**
     * Resolves a path according to the project, this will resolve linked resources
     * 
     * @param path
     * @return the resolved path
     */
    Path getFile(Path path);

    /**
     * Resolves a path according to the project, this will resolve linked resources
     * 
     * @param path
     * @return the resolved path
     */
    Path getFile(String path);

    Collection<ProjectVariable> getVariables();
}
