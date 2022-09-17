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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.PackagingType;

@Component(role = ArtifactHandler.class, hint = PackagingType.TYPE_P2_IU)
public class P2InstallableUnitArtifactHandler extends AbstractArtifactHandler {

	public P2InstallableUnitArtifactHandler() {
		super(PackagingType.TYPE_P2_IU, EXTENSION_ZIP, LANGUAGE_XML, false);
	}

}
