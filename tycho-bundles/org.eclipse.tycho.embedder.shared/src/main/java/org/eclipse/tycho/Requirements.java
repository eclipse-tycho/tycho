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

/**
 * {@link Requirements} is the "maven-world" abstraction of P2 IRequirement(s) and is simply an
 * accessory to data stored in {@link IDependencyMetadata}.
 *
 */
public interface Requirements {

    List<RequiredCapability> getRequiredCapabilities(IDependencyMetadata dependencyMetadata,
            DependencyMetadataScope... types);
}
