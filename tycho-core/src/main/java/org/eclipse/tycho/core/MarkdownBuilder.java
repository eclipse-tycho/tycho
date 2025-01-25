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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;

public class MarkdownBuilder {

    private final List<String> lines = new ArrayList<>();
    private Path output;

    public MarkdownBuilder(File output) {
        this(output == null ? (Path) null : output.toPath());
    }

    public MarkdownBuilder(Path output) {
        this.output = output;
    }

    public MarkdownBuilder add(String string) {
        lines.add(string);
        return this;
    }

    public MarkdownBuilder addListItem(String item) {
        lines.add("- " + item);
        return this;
    }

    public void write() throws MojoFailureException {
        if (output == null) {
            return;
        }
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, lines.stream().collect(Collectors.joining(System.lineSeparator())));
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
    }

    public void newLine() {
        lines.add("");
    }

    public void h1(String string) {
        lines.add("# " + string);
        lines.add("");
    }

    public void h2(String string) {
        lines.add("## " + string);
        lines.add("");
    }

    public void h3(String string) {
        lines.add("### " + string);
        lines.add("");
    }

}
