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
