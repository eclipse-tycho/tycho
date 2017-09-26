/*******************************************************************************
 * Copyright (c) 2012-2017 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Mickael Istria (Red Hat Inc.) - 522531 Baseline allows to ignore files
 *******************************************************************************/
package org.eclipse.tycho.artifactcomparator;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecution;

public interface ArtifactComparator {
    public ArtifactDelta getDelta(File baseline, File reactor, MojoExecution execution) throws IOException;
}
