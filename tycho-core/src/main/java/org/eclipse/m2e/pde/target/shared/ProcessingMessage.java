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

import org.eclipse.aether.artifact.Artifact;

/**
 * represents a message that occurred while processing the jar.
 */
public final class ProcessingMessage {

	private Artifact artifact;
	private Type type;
	private String message;

	ProcessingMessage(Artifact artifact, Type type, String message) {
		this.artifact = artifact;
		this.type = type;
		this.message = message;
	}

	public static enum Type {
		ERROR, WARN
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public String getMessage() {
		return message;
	}
	
	public Type getType() {
		return type;
	}
}
