/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho;

import java.io.File;

public interface BuildPropertiesParser {

    public static final String BUILD_PROPERTIES = "build.properties";

    default BuildProperties parse(ReactorProject project) {
        return parse(project.getBasedir(), project.getInterpolator());
    }

    /**
     * Parse the file "build.properties" in baseDir. If the file does not exist or cannot be read,
     * an "empty" {@link BuildProperties} will be returned.
     */
    BuildProperties parse(File baseDir, Interpolator interpolator);
}
