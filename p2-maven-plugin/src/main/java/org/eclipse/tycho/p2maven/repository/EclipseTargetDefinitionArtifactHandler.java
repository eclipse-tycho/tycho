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
package org.eclipse.tycho.p2maven.repository;

import org.eclipse.tycho.ArtifactType;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(ArtifactType.TYPE_ECLIPSE_TARGET_DEFINITION)
public class EclipseTargetDefinitionArtifactHandler extends AbstractArtifactHandler {

	public EclipseTargetDefinitionArtifactHandler() {
		super(ArtifactType.TYPE_ECLIPSE_TARGET_DEFINITION, "target", LANGUAGE_XML, false);
	}

}
