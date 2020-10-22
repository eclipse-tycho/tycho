/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.classpath;

import java.io.File;
import java.util.List;

public interface SourcepathEntry {
    public File getOutputDirectory();

    public File getSourcesRoot();

    /**
     * null means "everything included"
     */
    public List<String> getIncludes();

    /**
     * null means "nothing excluded"
     */
    public List<String> getExcludes();
}
