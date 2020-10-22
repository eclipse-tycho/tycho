/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

/**
 * Types of Eclipse/OSGi artifacts which can be referenced in a Tycho build.
 */
public final class ArtifactType {

    public static final String TYPE_ECLIPSE_PLUGIN = "eclipse-plugin";
    public static final String TYPE_ECLIPSE_FEATURE = "eclipse-feature";
    public static final String TYPE_ECLIPSE_PRODUCT = "eclipse-product";
    public static final String TYPE_INSTALLABLE_UNIT = "p2-installable-unit";

}
