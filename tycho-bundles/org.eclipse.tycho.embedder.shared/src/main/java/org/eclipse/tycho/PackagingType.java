/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

/**
 * Tycho's packaging types.
 */
public final class PackagingType {

    public static final String TYPE_ECLIPSE_PLUGIN = ArtifactType.TYPE_ECLIPSE_PLUGIN;
    public static final String TYPE_ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";
    public static final String TYPE_ECLIPSE_FEATURE = ArtifactType.TYPE_ECLIPSE_FEATURE;
    public static final String TYPE_ECLIPSE_UPDATE_SITE = "eclipse-update-site";
    public static final String TYPE_ECLIPSE_APPLICATION = "eclipse-application";
    public static final String TYPE_ECLIPSE_REPOSITORY = "eclipse-repository";
    public static final String TYPE_P2_IU = ArtifactType.TYPE_INSTALLABLE_UNIT;

}
