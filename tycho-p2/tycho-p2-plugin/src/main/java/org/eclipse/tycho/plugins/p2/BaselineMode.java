/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

public enum BaselineMode {
    /**
     * Disable baseline validation.
     */
    disable,

    /**
     * Warn about discrepancies between build and baseline artifacts but do not fail the build.
     */
    warn,

    /**
     * Fail the build if there are discrepancies between artifacts present both in build and
     * baseline. Attached artifacts only present in the build do not result in build failure.
     */
    failCommon,

    /**
     * Fail the build if there are any discrepancy between build and baseline artifacts.
     */
    fail;
}
