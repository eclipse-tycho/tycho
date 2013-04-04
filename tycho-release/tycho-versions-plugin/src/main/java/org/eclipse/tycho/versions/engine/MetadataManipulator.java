/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public interface MetadataManipulator {
    public Collection<String> validateChange(ProjectMetadata project, VersionChange change);

    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges);

    public boolean addMoreChanges(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges);

    public void writeMetadata(ProjectMetadata project) throws IOException;
}
