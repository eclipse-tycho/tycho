/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.ds;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DeclarativeServicesMojoTest {

	@TempDir
	Path tmp;

	@Test
	public void directoryWithZeroLengthIsClasspathEntry() throws Exception {
		// Windows reports a length of zero for directories
		File classes = new File(Files.createDirectory(tmp.resolve("classes")).toString()) {
			@Override
			public long length() {
				return 0;
			}
		};
		File output = tmp.resolve("output").toFile();
		assertTrue(DeclarativeServicesMojo.isClasspathEntry(classes, output));
	}

	@Test
	public void nonEmptyFileIsClasspathEntry() throws Exception {
		File jar = Files.write(tmp.resolve("lib.jar"), new byte[] { 1 }).toFile();
		assertTrue(DeclarativeServicesMojo.isClasspathEntry(jar, tmp.resolve("output").toFile()));
	}

	@Test
	public void outputDirectoryIsSkipped() throws Exception {
		File output = Files.createDirectory(tmp.resolve("output")).toFile();
		assertFalse(DeclarativeServicesMojo.isClasspathEntry(output, output));
	}

	@Test
	public void emptyOrMissingFilesAreSkipped() throws Exception {
		File output = tmp.resolve("output").toFile();
		File empty = Files.createFile(tmp.resolve("empty.jar")).toFile();
		assertFalse(DeclarativeServicesMojo.isClasspathEntry(empty, output));
		assertFalse(DeclarativeServicesMojo.isClasspathEntry(tmp.resolve("missing").toFile(), output));
	}
}
