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
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

@Named(XmlComparator.HINT)
@Singleton
public class XmlComparator implements ContentsComparator {

    static final String HINT = "xml";

    private static final Set<String> ALIAS = Set.of(HINT, "exsd", "genmodel", "xsd", "xsd2ecore", "ecore");

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        //if they differ make a more detailed comparision
        try {
            //TODO can we feed xsds to have default elements compared/normalized?
            //For example in a DS-XML one has cardinality = "1..1" is the same as having not an attribute at all
            //see issue https://github.com/xmlunit/xmlunit/issues/88
            //Another option would be to somehow implement this by our own...
            Diff baselineDiff = computeDiff(baseline, reactor);
            if (baselineDiff.hasDifferences()) {
                String message = baselineDiff.fullDescription();
                return TextComparator.createDelta(message, baseline, reactor, data);
            }
            return null;
        } catch (RuntimeException e) {
            return TextComparator.createDelta(ArtifactDelta.DEFAULT.getMessage(), baseline, reactor, data);
        }
    }

    private Diff computeDiff(InputStream baseline, InputStream reactor) {
        return DiffBuilder.compare(Input.fromStream(baseline))//
                .withTest(Input.fromStream(reactor))//
                .checkForSimilar()//
                .ignoreComments() //
                .ignoreWhitespace().build();
    }

    @Override
    public boolean matches(String nameOrExtension) {
        return ALIAS.contains(nameOrExtension.toLowerCase());
    }

}
