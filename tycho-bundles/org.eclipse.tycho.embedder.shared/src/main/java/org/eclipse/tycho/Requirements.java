/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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
package org.eclipse.tycho;

import java.util.List;

import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;

public interface Requirements {

    List<RequiredCapability> getRequiredCapabilities(IDependencyMetadata dependencyMetadata,
            DependencyMetadataType... types);
}
