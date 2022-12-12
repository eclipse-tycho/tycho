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

    public static class ComparisonData {

        public ComparisonData(List<String> ignoredPattern, boolean writeDelta) {
            this.ignoredPattern = ignoredPattern != null ? List.copyOf(ignoredPattern) : List.of();
            this.writeDelta = writeDelta;
        }

        private final List<String> ignoredPattern;
        private boolean writeDelta;

        public List<String> ignoredPattern() {
            return ignoredPattern;
        }

        public boolean writeDelta() {
            return writeDelta;
        }
    }

    public ArtifactDelta getDelta(File baseline, File reactor, ComparisonData execution) throws IOException;
}
