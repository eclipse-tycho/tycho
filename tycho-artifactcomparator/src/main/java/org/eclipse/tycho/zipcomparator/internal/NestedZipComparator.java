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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

@Component(role = ContentsComparator.class, hint = NestedZipComparator.TYPE)
public class NestedZipComparator implements ContentsComparator {
    public static final String TYPE = "zip";

    @Requirement(hint = ZipComparatorImpl.TYPE)
    private ArtifactComparator zipComparator;

    @Override
    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor, ComparisonData data) throws IOException {
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

}
