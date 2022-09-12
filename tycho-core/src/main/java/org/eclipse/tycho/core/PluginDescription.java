/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
package org.eclipse.tycho.core;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.model.PluginRef;

/**
 * Describes Eclipse plugin jar in context of aggregator project like eclipse-feature.
 * 
 * @author igor
 */
public interface PluginDescription extends ArtifactDescriptor {

    PluginRef getPluginRef();

}
