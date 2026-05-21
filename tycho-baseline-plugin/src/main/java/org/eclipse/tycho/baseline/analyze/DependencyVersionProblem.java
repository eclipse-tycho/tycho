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
package org.eclipse.tycho.baseline.analyze;

import java.util.Collection;
import java.util.List;

/**
 * Record for dependency version problems.
 *
 * @param key        a unique key identifying the dependency and version
 * @param message    the human-readable problem description
 * @param references the class names that reference the missing method
 * @param provided   the method signatures actually provided by the checked
 *                   version, may be {@code null}
 */
public record DependencyVersionProblem(String key, String message, Collection<String> references,
		List<MethodSignature> provided) {
}