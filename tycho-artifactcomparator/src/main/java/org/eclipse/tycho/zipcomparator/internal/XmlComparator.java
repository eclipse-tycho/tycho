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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

@Component(role = ContentsComparator.class, hint = XmlComparator.XML)
public class XmlComparator implements ContentsComparator {

    static final String XML = "xml";

    /**
     * System property that control if a detailed diff is desired or not (default = off)
     */
    private static final boolean NO_DIFF_DETAILS = Boolean.getBoolean("tycho.comparator.xml.noDiff");

    /**
     * System property that controls the threshold size where a direct byte compare is performed
     * (default 5 mb)
     */
    private static final int THRESHOLD = Integer.getInteger("tycho.comparator.xml.threshold", 1024 * 1024 * 5);

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        if (baseline.size() > THRESHOLD || reactor.size() > THRESHOLD) {
            return baseline.compare(reactor);
        }
        //if they differ make a more detailed comparision
        try {
            //TODO can we feed xsds to have default elements compared/normalized?
            //For example in a DS-XML one has cardinality = "1..1" is the same as having not an attribute at all
            //see issue https://github.com/xmlunit/xmlunit/issues/88
            //Another option would be to somehow implement this by our own...
            Diff baselineDiff = computeDiff(baseline, reactor);
            if (baselineDiff.hasDifferences()) {
                String message = baselineDiff.fullDescription();
                return createDelta(message, baseline, reactor);
            }
            return null;
        } catch (RuntimeException e) {
            return createDelta(ArtifactDelta.DEFAULT.getMessage(), baseline, reactor);
        }
    }

    private Diff computeDiff(InputStream baseline, InputStream reactor) {
        Diff baselineDiff = DiffBuilder.compare(Input.fromStream(baseline))//
                .withTest(Input.fromStream(reactor))//
                .checkForSimilar()//
                .ignoreComments() //
                .ignoreWhitespace().build();
        return baselineDiff;
    }

    private static ArtifactDelta createDelta(String message, ComparatorInputStream baseline,
            ComparatorInputStream reactor) {
        if (NO_DIFF_DETAILS) {
            return ArtifactDelta.DEFAULT;
        }
        String detailed;
        try {
            List<String> source = IOUtils.readLines(baseline.asNewStream(), StandardCharsets.UTF_8);
            List<String> target = IOUtils.readLines(reactor.asNewStream(), StandardCharsets.UTF_8);
            Patch<String> patch = DiffUtils.diff(source, target);
            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("baseline", "reactor", source, patch, 0);
            detailed = unifiedDiff.stream().collect(Collectors.joining((System.lineSeparator())));
        } catch (Exception e) {
            detailed = message;
        }
        return new SimpleArtifactDelta(message, detailed, baseline.asString(StandardCharsets.UTF_8),
                reactor.asString(StandardCharsets.UTF_8));
    }

}
