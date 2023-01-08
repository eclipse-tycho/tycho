/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.ds;

public enum HeaderConfiguration {
	/**
	 * If the header is not present, it is added, otherwise it is kept as is
	 */
	auto,
	/**
	 * Keep the header and never change it
	 */
	keep,
	/**
	 * Replace the header regardless of current manifest state
	 */
	replace;
}
