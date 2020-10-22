/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sebastien Arod - Initial implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

/**
 * @author sarod
 *
 */
public interface VersionRangeUpdateStrategy {

    public String computeNewVersionRange(String originalVersionRange, String originalReferencedVersion,
            String newReferencedVersion);

    public ImportRefVersionConstraint computeNewImportRefVersionConstraint(
            ImportRefVersionConstraint originalVersionConstraint, String originalReferencedVersion,
            String newReferencedVersion);
}
