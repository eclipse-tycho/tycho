/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.repository;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactType;

@Component(role = ArtifactHandler.class, hint = ArtifactType.TYPE_ECLIPSE_TARGET_DEFINITION)
public class EclipseTargetDefinitionArtifactHandler extends AbstractArtifactHandler {

	public EclipseTargetDefinitionArtifactHandler() {
		super(ArtifactType.TYPE_ECLIPSE_TARGET_DEFINITION, "target", LANGUAGE_XML, false);
	}

}
