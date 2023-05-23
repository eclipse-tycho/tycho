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
import java.util.stream.Stream;

import org.eclipse.aether.graph.DependencyNode;

import aQute.bnd.osgi.Jar;

public final class WrappedBundle {

	private DependencyNode node;
	private List<WrappedBundle> depends;
	private String instructionsKey;
	private Path file;
	private Jar jar;
	private List<ProcessingMessage> messages;

	WrappedBundle(DependencyNode node, List<WrappedBundle> depends, String key, Path file, Jar jar,
			List<ProcessingMessage> messages) {
		this.node = node;
		this.depends = depends;
		this.instructionsKey = key;
		this.file = file;
		this.jar = jar;
		this.messages = messages;
	}

	String getInstructionsKey() {
		return instructionsKey;
	}

	Jar getJar() {
		return jar;
	}

	/**
	 * @return the file where the wrappes bundle is located
	 */
	public Path getFile() {
		return file;
	}

	/**
	 * @return the messages that where produced
	 */
	public Stream<ProcessingMessage> messages() {
		return Stream.concat(messages.stream(), depends.stream().flatMap(dep -> dep.messages()));
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
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WrappedBundle other = (WrappedBundle) obj;
		return Objects.equals(instructionsKey, other.instructionsKey) && Objects.equals(node, other.node);
	}

}