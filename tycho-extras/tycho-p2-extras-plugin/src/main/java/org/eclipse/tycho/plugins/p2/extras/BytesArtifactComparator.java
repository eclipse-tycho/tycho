/*******************************************************************************
 * Copyright (c) 2017, 2022 Red Hat Inc., and others
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.zipcomparator.internal.SimpleArtifactDelta;

@Named(BytesArtifactComparator.HINT)
@Singleton
public class BytesArtifactComparator implements ArtifactComparator {

    public static final String HINT = "bytes";

    @Override
    public ArtifactDelta getDelta(File baseline, File reactor, ComparisonData data) throws IOException {
        if (FileUtils.contentEquals(baseline, reactor)) {
            return null;
        }
        return new SimpleArtifactDelta("Baseline " + baseline.getAbsolutePath() + " and reactor "
                + reactor.getAbsolutePath() + " are made of different bytes", baseline.getAbsolutePath(),
                reactor.getAbsolutePath());
    }

}
