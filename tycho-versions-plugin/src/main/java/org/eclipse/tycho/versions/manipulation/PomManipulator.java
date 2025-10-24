/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
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
package org.eclipse.tycho.versions.manipulation;

import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.pom.PomFile;

public interface PomManipulator extends MetadataManipulator {
    String HINT = "pom";

    void applyPropertyChange(String pomName, PomFile pom, String propertyName, String propertyValue);
}
