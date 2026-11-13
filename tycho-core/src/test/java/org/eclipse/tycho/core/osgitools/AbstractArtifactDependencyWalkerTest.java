/*******************************************************************************
 * Copyright (c) 2026 Christopher Lake and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christopher Lake - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AbstractArtifactDependencyWalkerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testMissingOptionalFeatureIsIgnored() throws Exception {
        List<String> missing = walk("optional=\"true\"");
        assertEquals(List.of(), missing);
    }

    @Test
    public void testMissingRequiredFeatureIsReported() throws Exception {
        List<String> missing = walk("");
        assertEquals(List.of("test.missing"), missing);
    }

    private List<String> walk(String includeAttributes) throws Exception {
        File location = tempDir.toFile();
        Files.writeString(tempDir.resolve("feature.xml"), """
                <feature id="test.root" version="1.0.0">
                  <includes id="test.missing" version="0.0.0" %s/>
                </feature>
                """.formatted(includeAttributes), StandardCharsets.UTF_8);
        Feature feature = Feature.read(new File(location, "feature.xml"));

        ArtifactDescriptor root = mock(ArtifactDescriptor.class);
        when(root.getKey()).thenReturn(new DefaultArtifactKey("eclipse-feature", "test.root", "1.0.0"));
        when(root.getLocation(true)).thenReturn(location);
        when(root.getInstallableUnits()).thenReturn(Set.of());
        DependencyArtifacts artifacts = mock(DependencyArtifacts.class);
        when(artifacts.getArtifacts()).thenReturn(List.of(root));

        List<String> missing = new ArrayList<>();
        ArtifactDependencyVisitor visitor = new ArtifactDependencyVisitor() {
            @Override
            public void missingFeature(FeatureRef ref, List<ArtifactDescriptor> walkback) {
                missing.add(ref.getId());
            }
        };
        new AbstractArtifactDependencyWalker(artifacts) {
            @Override
            public void walk(ArtifactDependencyVisitor v) {
                throw new UnsupportedOperationException();
            }
        }.traverseFeature(location, feature, visitor);
        return missing;
    }
}
