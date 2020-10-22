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

package org.eclipse.tycho.core.shared;

import java.io.File;

public interface BuildPropertiesParser {

    public static final String BUILD_PROPERTIES = "build.properties";

    /**
     * Parse the file "build.properties" in baseDir. If the file does not exist or cannot be read,
     * an "empty" {@link BuildProperties} will be returned.
     */
    public BuildProperties parse(File baseDir);
}
