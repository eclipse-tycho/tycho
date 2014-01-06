/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;

/**
 * All values (GAV, project base directory, and target directory) by which a Tycho reactor project
 * can be uniquely identified.
 */
public interface ReactorProjectIdentities {

    public String getGroupId();

    public String getArtifactId();

    public String getVersion();

    public File getBasedir();

    public BuildOutputDirectory getBuildDirectory();

    // TODO equals & hashCode contract
}
