/*******************************************************************************
 * Copyright (c) 2012, 2020 Sonatype Inc. and others.
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
package org.eclipse.tycho.jarcomparator.tests;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.zipcomparator.internal.CompoundArtifactDelta;
import org.eclipse.tycho.zipcomparator.internal.SimpleArtifactDelta;
import org.junit.Test;

public class CompoundArtifactDeltaTest {

    @Test
    public void testGetDetailedMessage() {
        Map<String, ArtifactDelta> manifest = new TreeMap<>();
        manifest.put("name1", new SimpleArtifactDelta("present in baseline only"));

        Map<String, ArtifactDelta> main = new TreeMap<>();
        main.put("path/file1", new SimpleArtifactDelta("different"));
        main.put("path/file2", new SimpleArtifactDelta("not present in baseline"));
        main.put("META-INF/MANIFEST.MF", new CompoundArtifactDelta("different", manifest));

        Map<String, ArtifactDelta> delta = new TreeMap<>();
        delta.put("<main>", new CompoundArtifactDelta("different", main));
        delta.put("sources", new SimpleArtifactDelta("different"));

        ArtifactDelta subject = new CompoundArtifactDelta(
                "Baseline and reactor artifacts have the same version but different contents", delta);

        String nl = System.lineSeparator();
        String expected = "Baseline and reactor artifacts have the same version but different contents" + nl
                + "   <main>: different" + nl //
                + "      META-INF/MANIFEST.MF: different" + nl //
                + "         name1: present in baseline only" + nl //
                + "      path/file1: different" + nl //
                + "      path/file2: not present in baseline" + nl //
                + "   sources: different" + nl;

        assertEquals(expected, subject.getDetailedMessage());
    }
}
