/*******************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests;

public record ExpectedBundle(String bsn, String version, boolean isSourceBundle, boolean isOriginal,
		ArtifactKey key) implements ExpectedUnit {

	@Override
	public String id() {
		return bsn();
	}

	@Override
	public String toString() {
		return bsn + ":" + version;
	}
}