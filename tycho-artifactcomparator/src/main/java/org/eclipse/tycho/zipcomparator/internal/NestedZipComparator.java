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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

@Named(NestedZipComparator.TYPE)
@Singleton
public class NestedZipComparator implements ContentsComparator {
    public static final String TYPE = "zip";

    @Inject
    @Named(ZipComparatorImpl.TYPE)
    private ArtifactComparator zipComparator;

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        Path baselineZip = Files.createTempFile("baseline", ".zip");
        Path reactorZip = Files.createTempFile("reactor", ".zip");
        try {
            Files.copy(baseline, baselineZip, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(reactor, reactorZip, StandardCopyOption.REPLACE_EXISTING);
            return zipComparator.getDelta(baselineZip.toFile(), reactorZip.toFile(), data);
        } finally {
            Files.deleteIfExists(baselineZip);
            Files.deleteIfExists(reactorZip);
        }
    }

    @Override
    public boolean matches(String extension) {
        return TYPE.equalsIgnoreCase(extension) || "jar".equalsIgnoreCase(extension) || "war".equalsIgnoreCase(extension);
    }

}
