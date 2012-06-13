/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.jarcomparator.tests;

import java.util.Map;
import java.util.TreeMap;

import junit.framework.Assert;

import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.zipcomparator.internal.CompoundArtifactDelta;
import org.eclipse.tycho.zipcomparator.internal.SimpleArtifactDelta;
import org.junit.Test;

public class CompoundArtifactDeltaTest {

    @Test
    public void testGetDetailedMessage() {
        Map<String, ArtifactDelta> manifest = new TreeMap<String, ArtifactDelta>();
        manifest.put("name1", new SimpleArtifactDelta("present in baseline only"));

        Map<String, ArtifactDelta> main = new TreeMap<String, ArtifactDelta>();
        main.put("path/file1", new SimpleArtifactDelta("different"));
        main.put("path/file2", new SimpleArtifactDelta("not present in baseline"));
        main.put("META-INF/MANIFEST.MF", new CompoundArtifactDelta("different", manifest));

        Map<String, ArtifactDelta> delta = new TreeMap<String, ArtifactDelta>();
        delta.put("<main>", new CompoundArtifactDelta("different", main));
        delta.put("sources", new SimpleArtifactDelta("different"));

        ArtifactDelta subject = new CompoundArtifactDelta(
                "Baseline and reactor artifacts have the same version but different contents", delta);

        String expected = "Baseline and reactor artifacts have the same version but different contents\n" //
                + "   <main>: different\n" //
                + "      META-INF/MANIFEST.MF: different\n" //
                + "         name1: present in baseline only\n" //
                + "      path/file1: different\n" //
                + "      path/file2: not present in baseline\n" //
                + "   sources: different\n";

        Assert.assertEquals(expected, subject.getDetailedMessage());
    }
}
