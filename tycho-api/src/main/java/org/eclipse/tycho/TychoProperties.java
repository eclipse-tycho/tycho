/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

/**
 * Properties that Tycho sets for use of other mojos or project configuration.
 */
public final class TychoProperties {

    public static final String BUILD_QUALIFIER = "buildQualifier";
    public static final String UNQUALIFIED_VERSION = "unqualifiedVersion";
    /**
     * The Eclipse version of a project with 'qualifier' literals expanded to the build qualifier.
     */
    public static final String QUALIFIED_VERSION = "qualifiedVersion";

}
