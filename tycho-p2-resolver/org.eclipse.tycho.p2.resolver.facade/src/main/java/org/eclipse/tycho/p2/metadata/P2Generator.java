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
package org.eclipse.tycho.p2.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface P2Generator {
    /**
     * @param artifacts
     * @param artifactsToBeAttached
     *            The passed data maps maven artifact classifier to artifacts. It is intended for
     *            adding additional artifacts during meta-data generation. Artifacts in this map
     *            will be attached with given classifier to the maven project for which meta-data is
     *            generated.
     * @param targetDir
     *            location to store artifacts created during meta data generation (e.g. root file
     *            zip)
     * @throws IOException
     */
    public void generateMetadata(List<IArtifactFacade> artifacts, Map<String, IArtifactFacade> artifactsToBeAttached,
            File targetDir) throws IOException;
}
