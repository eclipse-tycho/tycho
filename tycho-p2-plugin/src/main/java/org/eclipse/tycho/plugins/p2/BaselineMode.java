/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
