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

    /**
     * The windowing system <tt>osgi.ws</tt> of the machine that is running the build.
     */
    public static final String TYCHO_ENV_OSGI_WS = "tycho.env.osgi.ws";
    /**
     * The operating system <tt>osgi.os</tt> of the machine that is running the build.
     */
    public static final String TYCHO_ENV_OSGI_OS = "tycho.env.osgi.os";
    /**
     * The processor architecture <tt>osgi.arch</tt> of the machine that is running the build.
     */
    public static final String TYCHO_ENV_OSGI_ARCH = "tycho.env.osgi.arch";

}
