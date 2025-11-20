/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.sourceBundle;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * Integration test for multi-release JAR source bundle support.
 * 
 * Verifies that source folders with 'release' attributes in .classpath are properly
 * included in source bundles with the correct target path (META-INF/versions/<release>/).
 */
public class MultiReleaseSourceBundleTest extends AbstractTychoIntegrationTest {

    @Test
    public void testMultiReleaseSourceBundle() throws Exception {
        Verifier verifier = getVerifier("/sourceBundle.multiRelease", false);
        verifier.executeGoals(List.of("clean", "package"));
        verifier.verifyErrorFreeLog();
        
        File sourceBundle = new File(verifier.getBasedir(), 
                "target/bundle.multiRelease-1.0.0-SNAPSHOT-sources.jar");
        assertTrue("Missing expected source bundle: " + sourceBundle, sourceBundle.exists());
        
        try (ZipFile zip = new ZipFile(sourceBundle)) {
            // Verify base source files are included
            assertTrue("Base source Main.java not found", 
                    findEntry(zip, "tycho/mr/example/Main.java").isPresent());
            assertTrue("Base source HttpClient.java not found", 
                    findEntry(zip, "tycho/mr/example/HttpClient.java").isPresent());
            
            // Verify multi-release source files for Java 9 are included
            assertTrue("Multi-release source for Java 9 not found", 
                    findEntry(zip, "META-INF/versions/9/tycho/mr/example/HttpClient.java").isPresent());
            
            // Verify multi-release source files for Java 11 are included
            assertTrue("Multi-release source for Java 11 not found", 
                    findEntry(zip, "META-INF/versions/11/tycho/mr/example/HttpClient.java").isPresent());
        }
    }
    
    private static Optional<ZipEntry> findEntry(ZipFile zip, String name) {
        Stream<ZipEntry> stream = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(zip.entries().asIterator(), Spliterator.ORDERED), false);
        return stream.filter(e -> e.getName().equals(name)).findAny();
    }
}
