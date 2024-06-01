/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.shared;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.aether.graph.DependencyNode;

import aQute.bnd.osgi.Jar;

public final class WrappedBundle {

    private final DependencyNode node;
    private final List<WrappedBundle> depends;
    private final String instructionsKey;
    private final Optional<Path> file;
    private final Optional<Jar> jar;
    private final List<ProcessingMessage> messages;

    WrappedBundle(DependencyNode node, List<WrappedBundle> depends, String key, Path file, Jar jar,
            List<ProcessingMessage> messages) {
        this.node = node;
        this.depends = depends;
        this.instructionsKey = key;
        this.file = Optional.ofNullable(file);
        this.jar = Optional.ofNullable(jar);
        this.messages = messages;
    }

    String getInstructionsKey() {
        return instructionsKey;
    }

    Optional<Jar> getJar() {
        return jar;
    }

    DependencyNode getNode() {
        return node;
    }

    /**
     * 
     * @return an optional describing the wrapped bundle, or an empty optional if the bundle was not
     *         wrapped because of errors in the generation phase.
     */
    public Optional<Path> getFile() {
        return file;
    }

    /**
     * @param includeDependent
     *            if <code>true</code> includes messages from dependent items.
     * @return the messages that where produced
     */
    public Stream<ProcessingMessage> messages(boolean includeDependent) {
        if (includeDependent) {
            return Stream.concat(messages.stream(), depends.stream().flatMap(dep -> dep.messages(true)));
        }
        return messages.stream();
    }

    @Override
    public int hashCode() {
        return Objects.hash(instructionsKey, node);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof WrappedBundle other //
                && Objects.equals(instructionsKey, other.instructionsKey) //
                && Objects.equals(node, other.node);
    }

}
