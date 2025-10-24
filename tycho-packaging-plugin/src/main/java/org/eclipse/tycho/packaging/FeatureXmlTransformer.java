/*******************************************************************************
 * Copyright (c) 2011, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #845 - Feature restrictions are not taken into account when using emptyVersion
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.model.Feature;

public interface FeatureXmlTransformer {

	Feature expandReferences(Feature feature, TargetPlatform targetPlatform) throws MojoFailureException;
}
