/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
