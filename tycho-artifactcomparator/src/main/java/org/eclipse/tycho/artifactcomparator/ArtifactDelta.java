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
package org.eclipse.tycho.artifactcomparator;

import org.eclipse.tycho.zipcomparator.internal.SimpleArtifactDelta;

/**
 * Represents both simple and compound artifact delta.
 */
public interface ArtifactDelta {

    /**
     * a default instance that indicates there is a difference but can't tell any further details
     */
    public static final SimpleArtifactDelta DEFAULT = new SimpleArtifactDelta("different");

    /**
     * a default instance that indicate an item is only present in the baseline but no more details
     */
    public static final SimpleArtifactDelta BASELINE_ONLY = new SimpleArtifactDelta("present in baseline only");

    /**
     * a default instance that indicate an item is missing from what is found in the baseline
     */
    public static final SimpleArtifactDelta MISSING_FROM_BASELINE = new SimpleArtifactDelta("not present in baseline");

    /**
     * @return description of the delta, never null.
     */
    public String getMessage();

    /**
     * @return detailed description of the delta, never null.
     */
    public String getDetailedMessage();

}
