/*******************************************************************************
 * Copyright (c) 2025, 2025 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.repository;

public enum P2RepositoryKind {
	/** Kind representing a metadata repository. */
    metadata,
	/** Kind representing a artifact repository. */
    artifact;
}
