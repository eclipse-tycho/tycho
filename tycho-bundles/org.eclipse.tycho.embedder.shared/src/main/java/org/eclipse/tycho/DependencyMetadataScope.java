/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

/**
 * {@link DependencyMetadataScope} refers to the scope where a dependency and/or requirement is
 * relevant. For example some requirements are relevant for the initial resolving of a project (this
 * can be seen s the minimal set of dependencies required to actually work with, e.g. to validate
 * that all prerequisites are meet), others might be relevant for compiling (e.g. classpath entries,
 * compiler levels) and others only for test execution (e.g. additional mock-services, fragments and
 * so on).
 */
public enum DependencyMetadataScope {
    SEED, RESOLVE, COMPILE, TEST;
}
