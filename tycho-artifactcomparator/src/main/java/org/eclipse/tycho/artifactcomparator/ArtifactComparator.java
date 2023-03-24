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
 *    Mickael Istria (Red Hat Inc.) - 522531 Baseline allows to ignore files
 *******************************************************************************/
package org.eclipse.tycho.artifactcomparator;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ArtifactComparator {

    public static record ComparisonData(List<String> ignoredPattern, boolean writeDelta, boolean showDiffDetails) {

        /**
         * System property that control if a detailed diff is desired or not, <code>false</code>
         * (default) = no detailed diff is shown, <code>true</code> show detailed difference.
         */
        private static final boolean SHOW_DIFF_DETAILS = Boolean.getBoolean("tycho.comparator.showDiff");

        public ComparisonData(List<String> ignoredPattern, boolean writeDelta) {
            this(ignoredPattern, writeDelta, SHOW_DIFF_DETAILS);
        }

        public ComparisonData(List<String> ignoredPattern, boolean writeDelta, boolean showDiffDetails) {
            this.ignoredPattern = ignoredPattern != null ? List.copyOf(ignoredPattern) : List.of();
            this.writeDelta = writeDelta;
            this.showDiffDetails = showDiffDetails;
        }
    }

    public ArtifactDelta getDelta(File baseline, File reactor, ComparisonData execution) throws IOException;
}
