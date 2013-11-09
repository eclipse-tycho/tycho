/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

/**
 * All values (GAV or target directory location) by which a reactor project can be uniquely
 * identified.
 */
public interface ReactorProjectIdentities {

    public String getGroupId();

    public String getArtifactId();

    public String getVersion();

    public BuildOutputDirectory getBuildDirectory();

    // TODO equals & hashCode contract
}
