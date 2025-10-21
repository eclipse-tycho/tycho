/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.framework;

import org.apache.maven.model.Repository;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetPlatform;

public interface EclipseApplicationManager {

    EclipseApplication getApplication(TargetPlatform targetPlatform, Bundles bundles, Features features,
            String name);

    EclipseApplication getApplication(Repository location, Bundles bundles, Features features, String name);

    EclipseApplication getApplication(MavenRepositoryLocation repository, Bundles bundles, Features features,
            String name);

}
