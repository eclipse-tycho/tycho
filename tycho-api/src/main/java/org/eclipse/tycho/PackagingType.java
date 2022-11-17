/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.util.Set;

/**
 * Tycho's packaging types.
 */
public final class PackagingType {

    public static final String TYPE_ECLIPSE_PLUGIN = ArtifactType.TYPE_ECLIPSE_PLUGIN;
    public static final String TYPE_ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";
    public static final String TYPE_ECLIPSE_FEATURE = ArtifactType.TYPE_ECLIPSE_FEATURE;
    public static final String TYPE_ECLIPSE_REPOSITORY = "eclipse-repository";
    public static final String TYPE_ECLIPSE_TARGET_DEFINITION = "eclipse-target-definition";
    public static final String TYPE_P2_IU = ArtifactType.TYPE_INSTALLABLE_UNIT;

    public static final Set<String> TYCHO_PACKAGING_TYPES = Set.of(PackagingType.TYPE_ECLIPSE_PLUGIN,
            TYPE_ECLIPSE_TEST_PLUGIN, TYPE_ECLIPSE_FEATURE, TYPE_ECLIPSE_REPOSITORY, TYPE_ECLIPSE_TARGET_DEFINITION);

}
