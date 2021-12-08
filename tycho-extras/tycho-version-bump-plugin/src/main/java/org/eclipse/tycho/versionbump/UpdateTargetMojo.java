/*******************************************************************************
 * Copyright (c) 2010, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *    Christoph Läubrich - Adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionFile;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionFile.IULocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionFile.Unit;

/**
 * Quick&dirty way to update .target file to use latest versions of IUs available from specified
 * metadata repositories.
 */
@Mojo(name = "update-target")
public class UpdateTargetMojo extends AbstractUpdateMojo {
    @Parameter(property = "project")
    private MavenProject project;
    @Parameter(property = "target")
    private File targetFile;

    @Override
    protected void doUpdate() throws IOException, URISyntaxException {

        TargetDefinitionFile target = TargetDefinitionFile.read(targetFile);

        for (TargetDefinition.Location location : target.getLocations()) {
            if (location instanceof IULocation) {
                IULocation locationImpl = (IULocation) location;

                for (TargetDefinition.Unit unit : locationImpl.getUnits()) {
                    Unit unitImpl = (Unit) unit;
                    unitImpl.setVersion("0.0.0");
                }
            }
        }
        resolutionContext.setEnvironments(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));
        resolutionContext.addTargetDefinition(target);
        P2ResolutionResult result = p2.getTargetPlatformAsResolutionResult(resolutionContext, executionEnvironment);

        Map<String, String> ius = new HashMap<>();
        for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
            ius.put(entry.getId(), entry.getVersion());
        }

        for (TargetDefinition.Location location : target.getLocations()) {
            if (location instanceof IULocation) {
                IULocation locationImpl = (IULocation) location;

                for (TargetDefinition.Unit unit : locationImpl.getUnits()) {
                    Unit unitImpl = (Unit) unit;

                    String version = ius.get(unitImpl.getId());
                    if (version != null) {
                        unitImpl.setVersion(version);
                    } else {
                        getLog().error("Resolution result does not contain root installable unit " + unit.getId());
                    }
                }
            }
        }

        TargetDefinitionFile.write(target, targetFile);
    }

    @Override
    protected File getFileToBeUpdated() {
        return targetFile;
    }

}
