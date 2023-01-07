/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

/**
 * Compares text-like files by ignoring there line ending styles
 */
@Component(role = ContentsComparator.class, hint = TextComparator.HINT)
public class TextComparator implements ContentsComparator {

    static final String HINT = "txt";

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        ByteIterator baselineIterator = new ByteIterator(baseline.asBytes());
        ByteIterator reactorIterator = new ByteIterator(reactor.asBytes());
        while (baselineIterator.hasNext() && reactorIterator.hasNext()) {
            if (baselineIterator.next() != reactorIterator.next()) {
                return createDelta(ArtifactDelta.DEFAULT.getMessage(), baseline, reactor);
            }
        }
        //now both need to be at the end of the stream if they are the same!
        if (baselineIterator.hasNext() || reactorIterator.hasNext()) {
            return createDelta(ArtifactDelta.DEFAULT.getMessage(), baseline, reactor);
        }
        return ArtifactDelta.NO_DIFFERENCE;
    }

    private static final class ByteIterator {

        private byte[] bytes;
        private int index;

        public ByteIterator(byte[] bytes) {
            this.bytes = bytes;
        }

        byte next() throws EOFException {
            if (hasNext()) {
                byte b = bytes[index];
                index++;
                return b;
            }
            throw new EOFException();
        }

        boolean hasNext() {
            skipNewLines();
            return index < bytes.length;
        }

        private void skipNewLines() {
            while (index < bytes.length) {
                byte b = bytes[index];
                if (b == '\n' || b == '\r') {
                    index++;
                    continue;
                }
                return;
            }
        }

    }

    @Override
    public boolean matches(String extension) {
        return HINT.equals(extension) ||
        //TODO is there a way to compare java files? See https://github.com/eclipse-jdt/eclipse.jdt.core/discussions/628
                "java".equalsIgnoreCase(extension) ||
                //TODO is there a better way to compare html? See for example https://stackoverflow.com/questions/47310845/compare-two-html-documents-using-jsoup-java 
                "html".equalsIgnoreCase(extension) || "htm".equalsIgnoreCase(extension);
    }

    public static ArtifactDelta createDelta(String message, ComparatorInputStream baseline,
            ComparatorInputStream reactor) {
        if (NO_DIFF_DETAILS) {
            return ArtifactDelta.DEFAULT;
        }
        String detailed;
        try {
            List<String> source = IOUtils.readLines(baseline.asNewStream(), StandardCharsets.UTF_8);
            List<String> target = IOUtils.readLines(reactor.asNewStream(), StandardCharsets.UTF_8);
            Patch<String> patch = DiffUtils.diff(source, target);
            List<String> unifiedDiffList = UnifiedDiffUtils.generateUnifiedDiff("baseline", "reactor", source, patch,
                    0);
            detailed = unifiedDiffList.stream().collect(Collectors.joining((System.lineSeparator())));
        } catch (Exception e) {
            detailed = message;
        }
        return new SimpleArtifactDelta(message, detailed, baseline.asString(StandardCharsets.UTF_8),
                reactor.asString(StandardCharsets.UTF_8));
    }

}
