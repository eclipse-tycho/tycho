/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;

public interface ManifestHelper {

    /**
     * Get the line number of the given manifest header
     * 
     * @param manifestFile
     * @param headerName
     * @return the line number or 0 if not found
     */
    int getLineNumber(File manifestFile, String headerName);

}
