/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.versions.engine;

import java.io.IOException;
import java.util.Collection;

public interface MetadataManipulator {

    public boolean addMoreChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext);

    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext);

    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext);

    public void writeMetadata(ProjectMetadata project) throws IOException;
}
