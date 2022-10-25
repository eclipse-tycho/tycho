/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *    Christoph Läubrich - Christoph Läubrich - Issue #502 - TargetDefinitionUtil / UpdateTargetMojo should not be allowed to modify the internal state of the target
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    protected void doUpdate() throws IOException, URISyntaxException, ParserConfigurationException, SAXException {

        Document target;
        try (FileInputStream input = new FileInputStream(targetFile)) {
            target = TargetDefinitionFile.parseDocument(input);
            TargetDefinitionFile parsedTarget = TargetDefinitionFile.parse(target, targetFile.getAbsolutePath());
            resolutionContext.setEnvironments(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));
            resolutionContext.addTargetDefinition(new LatestVersionTarget(parsedTarget));
            P2ResolutionResult result = p2.getTargetPlatformAsResolutionResult(resolutionContext, executionEnvironment);

            Map<String, String> ius = new HashMap<>();
            for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
                ius.put(entry.getId(), entry.getVersion());
            }
            //update <unit id="..." version="..."/>
            NodeList units = target.getElementsByTagName("unit");
            for (int i = 0; i < units.getLength(); i++) {
                Element unit = (Element) units.item(i);
                String id = unit.getAttribute("id");
                String version = ius.get(id);
                if (version != null) {
                    unit.setAttribute("version", version);
                } else {
                    getLog().error("Resolution result does not contain root installable unit " + id);
                }
            }
        }
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            TargetDefinitionFile.writeDocument(target, outputStream);
        }
    }

    @Override
    protected File getFileToBeUpdated() {
        return targetFile;
    }

    private static final class LatestVersionTarget implements TargetDefinition {

        private TargetDefinitionFile delegate;

        public LatestVersionTarget(TargetDefinitionFile delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<? extends Location> getLocations() {
            return delegate.getLocations().stream().map(location -> {
                if (location instanceof InstallableUnitLocation iuLocation) {
                    return new LatestVersionLocation(iuLocation);
                } else {
                    return location;
                }
            }).toList();
        }

        @Override
        public boolean hasIncludedBundles() {
            return delegate.hasIncludedBundles();
        }

        @Override
        public String getOrigin() {
            return delegate.getOrigin();
        }

        @Override
        public String getTargetEE() {
            return delegate.getTargetEE();
        }

    }

    private static final class LatestVersionLocation implements InstallableUnitLocation {

        private InstallableUnitLocation delegate;

        public LatestVersionLocation(InstallableUnitLocation delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<? extends Repository> getRepositories() {
            return delegate.getRepositories();
        }

        @Override
        public List<? extends TargetDefinition.Unit> getUnits() {
            return delegate.getUnits().stream().map(LatestVersionUnit::new).toList();
        }

        @Override
        public IncludeMode getIncludeMode() {
            return delegate.getIncludeMode();
        }

        @Override
        public boolean includeAllEnvironments() {
            return delegate.includeAllEnvironments();
        }

        @Override
        public boolean includeSource() {
            return delegate.includeSource();
        }

    }

    private static final class LatestVersionUnit implements TargetDefinition.Unit {

        private Unit delegate;

        public LatestVersionUnit(TargetDefinition.Unit delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public String getVersion() {
            return "0.0.0";
        }

    }

}
