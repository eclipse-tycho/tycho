/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

@Component(role = ContentsComparator.class, hint = XmlComparator.XML)
public class XmlComparator implements ContentsComparator {

    static final String XML = "xml";

    @Override
    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor, ComparisonData data) throws IOException {
        byte[] baselineBytes = IOUtils.toByteArray(baseline);
        byte[] reactorBytes = IOUtils.toByteArray(reactor);
        //TODO can we feed xsds to have default elements compared/normalized?
        //For example in a DS-XML one has cardinality = "1..1" is the same as having not an attribute at all
        //see issue https://github.com/xmlunit/xmlunit/issues/88
        //Another option would be to somehow implement this by our own...
        try {
            Diff baselineDiff = computeDiff(baselineBytes, reactorBytes);
            if (baselineDiff.hasDifferences()) {
                String message = baselineDiff.fullDescription();
                return createDelta(message, baselineBytes, reactorBytes);
            }
        } catch (RuntimeException e) {
            //in case of malformed xml we cannot compare it better than comparing bytes...
            if (!Arrays.equals(baselineBytes, reactorBytes)) {
                return createDelta("different", baselineBytes, reactorBytes);
            }
        }
        return null;
    }

    private Diff computeDiff(byte[] baselineBytes, byte[] reactorBytes) {
        Diff baselineDiff = DiffBuilder.compare(Input.fromByteArray(baselineBytes))//
                .withTest(Input.fromByteArray(reactorBytes))//
                .checkForSimilar()//
                .ignoreComments() //
                .ignoreWhitespace().build();
        return baselineDiff;
    }

    private static ArtifactDelta createDelta(String message, byte[] baselineBytes, byte[] reactorBytes) {
        String detailed;
        try {
            List<String> source = IOUtils.readLines(new ByteArrayInputStream(baselineBytes), StandardCharsets.UTF_8);
            List<String> target = IOUtils.readLines(new ByteArrayInputStream(reactorBytes), StandardCharsets.UTF_8);
            Patch<String> patch = DiffUtils.diff(source, target);
            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("baseline", "reactor", source, patch, 0);
            detailed = unifiedDiff.stream().collect(Collectors.joining((System.lineSeparator())));
        } catch (Exception e) {
            detailed = message;
        }
        return new SimpleArtifactDelta(message, detailed, new String(baselineBytes, StandardCharsets.UTF_8),
                new String(reactorBytes, StandardCharsets.UTF_8));
    }

}
