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
     * System property that control if a detailed diff is desired or not, <code>false</code>
     * (default) = no detailed diff is shown, <code>true</code> show detailed difference.
     */
    static final boolean SHOW_DIFF_DETAILS = Boolean.getBoolean("tycho.comparator.showDiff");

    /**
     * System property that controls the threshold size where a direct byte compare is performed
     * (default 5 mb)
     */
    static final int THRESHOLD = Integer.getInteger("tycho.comparator.threshold", 1024 * 1024 * 5);

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

    /**
     * Check if this comparator matches the given name or extension
     * 
     * @param nameOrExtension
     *            the extension or name to match
     * @return <code>true</code> if this comparator matches, <code>false</code> otherwise
     */
    boolean matches(String nameOrExtension);
}
