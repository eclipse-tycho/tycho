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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

/**
 * Compares text-like files by ignoring there line ending styles
 */
@Named(TextComparator.HINT)
@Singleton
public class TextComparator implements ContentsComparator {

    static final String HINT = "txt";

    private static final char CR = '\r';
    private static final char LF = '\n';

    // Possible new lines:
    // \n -- unix style
    // \r\n -- windows style
    // \r -- old Mac OS 9 style, recent Mac OS X/macOS use \n

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        return compareText(baseline, reactor, data);
    }

    public static ArtifactDelta compareText(ComparatorInputStream baseline, ComparatorInputStream reactor,
            ComparisonData data) {
        if (!isEqualTextIngoreNewLine(baseline.asBytes(), reactor.asBytes())) {
            return createDelta(ArtifactDelta.DEFAULT.getMessage(), baseline, reactor, data);
        }
        return ArtifactDelta.NO_DIFFERENCE;
    }

    /**
     * Tests if {@code baseline} and {@code reactor} contain equal text, if line-endings are
     * ignored.
     * 
     * @implNote This methods is intended to have the same results as if the entire content of each
     *           array were read and compared line by line using BufferedReader.readLine(), which
     *           only returns the line content, without terminators. The actual implementation is
     *           just more efficient, because it does not create String objects for the entire
     *           content.
     */
    public static boolean isEqualTextIngoreNewLine(byte[] baseline, byte[] reactor) {
        int indexBaseline = 0;
        int indexReactor = 0;
        int mismatch = Arrays.mismatch(baseline, reactor);
        while (mismatch >= 0) {
            indexBaseline += mismatch;
            indexReactor += mismatch;
            int baselineNewLine = newLineLength(baseline, indexBaseline);
            int reactorNewLine = newLineLength(reactor, indexReactor);
            if (baselineNewLine < 0 || reactorNewLine < 0) {
                return false;
            }
            // Both sliders are at either "\n" or "\r\n"
            indexBaseline += baselineNewLine;
            indexReactor += reactorNewLine;
            mismatch = Arrays.mismatch(baseline, indexBaseline, baseline.length, reactor, indexReactor, reactor.length);
        }
        return true;
    }

    private static int newLineLength(byte[] bytes, int index) {
        if (index < bytes.length) {
            if (bytes[index] == LF
                    // Prevent "\r\n" and "\r\r\n" from being treated as equals
                    && (index == 0 || bytes[index - 1] != CR)) {
                return 1;
            } else if (bytes[index] == CR) {
                return index + 1 < bytes.length && bytes[index + 1] == LF ? 2 : 1;
            }
        }
        return -1;
    }

    @Override
    public boolean matches(String nameOrExtension) {
        return HINT.equalsIgnoreCase(nameOrExtension);
    }

    public static ArtifactDelta createDelta(String message, ComparatorInputStream baseline,
            ComparatorInputStream reactor, ComparisonData data) {
        if (data.showDiffDetails()) {
            String detailed;
            try {
                List<String> source = IOUtils.readLines(baseline.asNewStream(), StandardCharsets.UTF_8);
                List<String> target = IOUtils.readLines(reactor.asNewStream(), StandardCharsets.UTF_8);
                Patch<String> patch = DiffUtils.diff(source, target);
                List<String> unifiedDiffList = UnifiedDiffUtils.generateUnifiedDiff("baseline", "reactor", source,
                        patch, 0);
                detailed = unifiedDiffList.stream().collect(Collectors.joining(System.lineSeparator()));
            } catch (Exception e) {
                detailed = message;
            }
            return new SimpleArtifactDelta(message, detailed, baseline.asString(StandardCharsets.UTF_8),
                    reactor.asString(StandardCharsets.UTF_8));
        }
        return ArtifactDelta.DEFAULT;
    }

}
