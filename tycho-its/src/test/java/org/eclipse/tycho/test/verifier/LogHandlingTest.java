/*******************************************************************************
 * Copyright (c) 2026 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers how {@link Verifier} quotes the log of a failed build and keeps the forked build's output
 * readable as UTF-8, without launching Maven.
 */
class LogHandlingTest {

	@TempDir
	Path dir;

	@Test
	void tailOfAHugeLogReadsOnlyItsEnd() throws Exception {
		Path log = dir.resolve("log.txt");
		try (BufferedWriter writer = Files.newBufferedWriter(log, StandardCharsets.UTF_8)) {
			for (int i = 0; i < 2_000_000; i++) {
				writer.write("[INFO] line " + i + "\n");
			}
		}
		assertTrue(Files.size(log) > 30_000_000L);

		String tail = Verifier.tail(log.toFile());

		String[] lines = tail.split("\n");
		assertEquals(200, lines.length);
		assertEquals("[INFO] line 1999800", lines[0]);
		assertEquals("[INFO] line 1999999", lines[199]);
	}

	@Test
	void tailOfAShortLogIsTheWholeLog() throws Exception {
		Path log = dir.resolve("log.txt");
		Files.writeString(log, "[INFO] first\r\n[ERROR] second\r\n", StandardCharsets.UTF_8);

		assertEquals("[INFO] first\n[ERROR] second", Verifier.tail(log.toFile()));
	}

	@Test
	void tailReplacesBytesThatAreNotUtf8() throws Exception {
		Path log = dir.resolve("log.txt");
		// "café" in windows-1252, as a forked build on Windows wrote it before the output was UTF-8
		Files.write(log, new byte[] { 'c', 'a', 'f', (byte) 0xE9, '\n' });

		assertEquals("caf�", Verifier.tail(log.toFile()));
	}

	@Test
	void tailOfAMissingLogSaysSo() {
		assertTrue(Verifier.tail(new File(dir.toFile(), "missing.txt")).startsWith("(log not readable: "));
	}

	@Test
	void mavenOptsAskForUtf8OutputAndKeepWhatIsThere() {
		assertEquals("-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8", Verifier.withUtf8Output(null));
		assertEquals("-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8", Verifier.withUtf8Output(" "));
		assertEquals("-Xmx1g -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8", Verifier.withUtf8Output("-Xmx1g"));
	}
}
