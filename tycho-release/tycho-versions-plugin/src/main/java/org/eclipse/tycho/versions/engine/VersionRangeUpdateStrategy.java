/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
