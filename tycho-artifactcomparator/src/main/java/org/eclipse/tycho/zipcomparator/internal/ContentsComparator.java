/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.zipcomparator.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

public interface ContentsComparator {
    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor, MojoExecution mojo) throws IOException;
}
