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

public enum BaselineReplace {
    /**
     * Do not replace build artifacts with baseline version.
     */
    none,

    /**
     * Replace build artifacts with baseline version. Attached artifacts only present in the build
     * are not removed and will likely result in inconsistencies among artifacts of the same
     * project! Use as last resort when baseline does not contain all build artifacts.
     */
    common,

    /**
     * Replace build artifacts with baseline version. Attached artifacts only present in the build
     * are removed.
     */
    all;
}
