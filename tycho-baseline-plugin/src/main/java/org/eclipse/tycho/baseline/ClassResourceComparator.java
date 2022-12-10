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
package org.eclipse.tycho.baseline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;

import org.codehaus.plexus.component.annotations.Component;

@Component(role = ResourceComparator.class, hint = "class")
public class ClassResourceComparator extends AbstractUnifiedDiff {

	@Override
	protected List<String> getLines(InputStream stream) throws IOException {
		Optional<ToolProvider> tool = ToolProvider.findFirst("javap");
		if (tool.isEmpty()) {
			return List.of();
		}
		ToolProvider javap = tool.get();
		Path tempFile = Files.createTempFile("compare", ".java");
		Path classFile = Files.createTempFile("compare", ".class");
		try (PrintWriter writer = new PrintWriter(tempFile.toFile())) {
			Files.copy(stream, classFile, StandardCopyOption.REPLACE_EXISTING);
			if (javap.run(writer, new PrintWriter(OutputStream.nullOutputStream()), "-c",
					classFile.toAbsolutePath().toString()) == 0) {
				return Files.readAllLines(tempFile, Charset.defaultCharset());
			}
		} finally {
			tempFile.toFile().delete();
			classFile.toFile().delete();
		}
		return List.of();
	}

}
