/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

public interface ContentsComparator {
    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor) throws IOException;
}
