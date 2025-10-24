/*******************************************************************************
 * Copyright (c) 2015, 2023 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *    Marco Lehmann-Mörz - issue #2877 - tycho-versions-plugin:bump-versions does not honor SNAPSHOT suffix
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.model.IU;

import de.pdark.decentxml.Element;

public interface IUXmlTransformer {

    void replaceSelfQualifiers(IU iu, String version, String qualifier);

    void replaceQualifierInCapabilities(List<Element> providedCapabilities, String qualifier);

    void replaceQualifierInRequirements(IU iu, TargetPlatform targetPlatform) throws MojoFailureException;

    void replaceZerosInRequirements(IU iu, TargetPlatform targetPlatform) throws MojoFailureException;

    void injectMavenProperties(IU iu, MavenProject project);

    void addSelfCapability(IU iu);
}
