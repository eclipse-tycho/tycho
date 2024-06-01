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
public record ProcessingMessage(Artifact artifact, Type type, String message) {

	public enum Type {
		ERROR, WARN 
	}

}
