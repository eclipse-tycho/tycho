/*******************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.IOException;
import java.util.Arrays;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

@Component(role = ContentsComparator.class, hint = TextContentsComparator.TYPE)
public class TextContentsComparator implements ContentsComparator {

    public static final String DEFAULT_TEXT_FILE_EXTENSIONS = "txt,java,html,xml,exsd";

    public static final String TYPE = "text";

    private static final char CR = '\r';
    private static final char LF = '\n';

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baselineStream, ComparatorInputStream reactorStream,
            ComparisonData data) throws IOException {
        byte[] baseline = baselineStream.asBytes();
        byte[] reactor = reactorStream.asBytes();

        int bI = 0;
        int rI = 0;
        int mismatch = Arrays.mismatch(baseline, reactor);
        while (mismatch >= 0) {
            int baselineNewLine = newLineLength(baseline, bI + mismatch);
            int reactorNewLine = newLineLength(reactor, rI + mismatch);
            if (baselineNewLine < 0 || reactorNewLine < 0) {
                return ArtifactDelta.DEFAULT;
            }
            bI += baselineNewLine;
            rI += reactorNewLine;

            mismatch = Arrays.mismatch(baseline, bI, baseline.length, reactor, rI, reactor.length);
        }
        return null;
    }

    private int newLineLength(byte[] bytes, int index) {
        if (index < bytes.length) {
            if (bytes[index] == LF) {
                return 1;
            } else if (bytes[index] == CR && index + 1 < bytes.length && bytes[index + 1] == LF) {
                return 2;
            }
        }
        return -1;
    }

}
