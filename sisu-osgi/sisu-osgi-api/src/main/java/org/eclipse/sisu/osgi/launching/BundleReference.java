/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.osgi.launching;

import java.io.File;

/**
 * A reference to a bundle, its version and location
 * 
 */
public interface BundleReference {

    /**
     * @return the id of this bundle, this usually match the bundle symbolic name
     */
    public String getId();

    /**
     * A Version in case multiple artifacts with the same version are to be added
     */
    public String getVersion();

    /**
     * @return location where the bundle is stored on disk
     */
    public File getLocation();

}
