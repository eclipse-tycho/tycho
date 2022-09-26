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
package org.eclipse.tycho.p2.facade;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Represents a p2 metadata repository. Facade only exposes serialized form (content.xml).
 * 
 * @TODO better class name
 */
public interface MetadataSerializable {

    /**
     * Writes the given set of installable units to the given output stream in standard p2 metadata
     * repository format. The caller is responsible for closing the stream.
     */
    void serialize(OutputStream stream, Set<IInstallableUnit> installableUnits) throws IOException;
}
