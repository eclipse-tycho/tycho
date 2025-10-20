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

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

@Named(DefaultContentsComparator.TYPE)
@Singleton
public class DefaultContentsComparator implements ContentsComparator {

    public static final String TYPE = "default";

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        if (isTextFile(baseline) && isTextFile(reactor)) {
            //If both items a certainly a text file, we compare them ignoring line endings
            return TextComparator.compareText(baseline, reactor, data);
        }
        return ArtifactDelta.DEFAULT;
    }

    /**
     * This works like the git-diff tool that determine if a file is binary to look for any NULL
     * byte in the file
     * 
     * @param stream
     * @return
     * @throws IOException
     */
    private static boolean isTextFile(ComparatorInputStream stream) throws IOException {
        try (InputStream is = stream.asNewStream()) {
            int i;
            while ((i = is.read()) > -1) {
                if (i == 0) {
                    //seems to be a binary file...
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean matches(String extension) {
        return false;
    }

}
