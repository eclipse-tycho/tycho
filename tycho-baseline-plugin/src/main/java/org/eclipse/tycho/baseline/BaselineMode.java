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

public enum BaselineMode {

    /**
	 * Warn about any deviation between build and baseline artifacts.
	 */
    warn,

	/**
	 * Fail the build if the artifact diverged in an incompatible way, but only warn
	 * about when there is no baseline (yet).
	 */
	evolve,

    /**
	 * Fail the build if there are any baseline problems.
	 */
    fail;
}
