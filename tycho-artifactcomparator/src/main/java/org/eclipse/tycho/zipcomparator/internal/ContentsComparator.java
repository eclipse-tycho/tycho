/*******************************************************************************
 * Copyright (c) 2012, 2022 Sonatype Inc. and others.
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

import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

public interface ContentsComparator {

    /**
     * Computes the delta for the given {@link InputStream}s, the streams passed will support
     * mark/reset for repeated reads.
     * 
     * @param baseline
     *            the baseline data
     * @param reactor
     *            the reactor data or current project state
     * @param data
     * @return the {@link ArtifactDelta} or {@link ArtifactDelta#NO_DIFFERENCE} if the content is
     *         semantically the same
     * @throws IOException
     */
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException;
}
