/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.model.classpath;

import java.io.File;
import java.util.OptionalInt;

public interface SourceFolderClasspathEntry extends ProjectClasspathEntry {

    /**
     * 
     * @return the source folder
     */
    File getSourcePath();

    /**
     * 
     * @return the configured output folder
     */
    File getOutputFolder();

    /**
     * @return the Java release version if this entry is marked as a multi-release source folder, or
     *         an empty OptionalInt if not
     */
    default OptionalInt getMultiReleaseVersion() {
        String release = getAttributes().get("release");
        if (release != null && !release.isBlank()) {
            try {
                return OptionalInt.of(Integer.parseInt(release));
            } catch (NumberFormatException e) {
                // invalid value, ignore
            }
        }
        return OptionalInt.empty();
    }
}
