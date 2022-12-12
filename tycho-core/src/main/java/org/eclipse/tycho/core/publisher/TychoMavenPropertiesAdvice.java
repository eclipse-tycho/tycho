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
 *    Christoph Läubrich - Issue #658 - preserve p2 artifact properties (eg PGP, maven info...)
 *******************************************************************************/
package org.eclipse.tycho.core.publisher;

import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2maven.advices.MavenPropertiesAdvice;

public class TychoMavenPropertiesAdvice extends MavenPropertiesAdvice {

    public TychoMavenPropertiesAdvice(IArtifactFacade artifactFacade, MavenContext mavenContext) {
        this(artifactFacade, artifactFacade.getClassifier(), mavenContext);
    }

    public TychoMavenPropertiesAdvice(IArtifactFacade artifactFacade, String classifier, MavenContext mavenContext) {
        super(artifactFacade.getGroupId(), artifactFacade.getArtifactId(), artifactFacade.getVersion(), classifier,
                mavenContext.getExtension(artifactFacade.getPackagingType()), artifactFacade.getPackagingType(),
                artifactFacade.getRepository());
    }
}
