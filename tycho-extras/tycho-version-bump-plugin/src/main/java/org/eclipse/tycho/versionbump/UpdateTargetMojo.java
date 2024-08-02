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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.p2resolver.TargetDefinitionVariableResolver;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinition.FollowRepositoryReferences;
import org.eclipse.tycho.targetplatform.TargetDefinition.IncludeMode;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Unit;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.eclipse.tycho.targetplatform.TargetPlatformArtifactResolver;
import org.eclipse.tycho.targetplatform.TargetResolveException;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

/**
 * Quick&dirty way to update .target file to use latest versions of IUs available from specified
 * metadata repositories.
 */
@Mojo(name = "update-target")
public class UpdateTargetMojo extends AbstractUpdateMojo {
    @Parameter(property = "target")
    private File targetFile;

    @Component
    private TargetDefinitionVariableResolver varResolver;

    @Component
    private MavenDependenciesResolver resolver;

    @Component
    private MavenSession mavenSession;

    @Override
    protected void doUpdate() throws IOException, URISyntaxException, ParserConfigurationException,
            TargetResolveException, MojoFailureException, VersionRangeResolutionException, ArtifactResolutionException {
        File file = getFileToBeUpdated();
        getLog().info("Update target file " + file);
        //we use the descent xml parser here because we need to retain the formating of the original file
        XMLParser parser = new XMLParser();
        Document target = parser.parse(new XMLIOSource(targetFile));
        boolean changed = false;
        try (FileInputStream input = new FileInputStream(file)) {
            TargetDefinitionFile parsedTarget = TargetDefinitionFile.read(file);
            resolutionContext.setEnvironments(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));
            resolutionContext.addTargetDefinition(new LatestVersionTarget(parsedTarget, varResolver));
            resolutionContext.setIgnoreLocalArtifacts(true);
            P2ResolutionResult result = p2.getTargetPlatformAsResolutionResult(resolutionContext, executionEnvironment);

            Map<String, String> ius = new HashMap<>();
            for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
                ius.put(entry.getId(), entry.getVersion());
            }
            for (Element iuLocation : getLocations("InstallableUnit", target)) {
                List<Element> children = iuLocation.getChildren("unit");
                for (Element unit : children) {
                    String id = unit.getAttributeValue("id");
                    String version = ius.get(id);
                    if (version != null) {
                        String currentVersion = unit.getAttributeValue("version");
                        if (version.equals(currentVersion)) {
                            getLog().debug("unit '" + id + "' is already up-to date");
                        } else {
                            changed = true;
                            getLog().info(
                                    "Update version of unit '" + id + "' from " + currentVersion + " > " + version);
                            unit.setAttribute("version", version);
                        }
                    } else {
                        getLog().warn("Resolution result does not contain root installable unit '" + id
                                + "' update is skipped!");
                    }
                }
            }
            for (Element mavenLocation : getLocations("Maven", target)) {
                Element dependencies = mavenLocation.getChild("dependencies");
                if (dependencies != null) {
                    for (Element dependency : dependencies.getChildren("dependency")) {
                        Dependency mavenDependency = new Dependency();
                        mavenDependency.setGroupId(getElementValue("groupId", dependency));
                        mavenDependency.setArtifactId(getElementValue("artifactId", dependency));
                        mavenDependency.setVersion(getElementValue("version", dependency));
                        mavenDependency.setType(getElementValue("type", dependency));
                        mavenDependency.setClassifier(getElementValue("classifier", dependency));
                        Artifact highestVersionArtifact = resolver.resolveHighestVersion(project, mavenSession,
                                mavenDependency);
                        String newVersion = highestVersionArtifact.getVersion();
                        if (newVersion.equals(mavenDependency.getVersion())) {
                            getLog().debug(mavenDependency + " is already up-to date");
                        } else {
                            changed = true;
                            setElementValue("version", newVersion, dependency);
                            getLog().info("update " + mavenDependency + " to version " + newVersion);
                        }
                    }
                }
            }
        }
        if (changed) {
            String enc = target.getEncoding() != null ? target.getEncoding() : "UTF-8";
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), enc); XMLWriter xw = new XMLWriter(w)) {
                try {
                    target.toXML(xw);
                } finally {
                    xw.flush();
                }
            }
        }
    }

    private void setElementValue(String name, String value, Element root) {
        Element child = root.getChild(name);
        if (child != null) {
            child.setText(value);
        }
    }

    private String getElementValue(String name, Element root) {
        Element child = root.getChild(name);
        if (child != null) {
            String text = child.getText().trim();
            if (text.isBlank()) {
                return null;
            }
            return text;
        }
        return null;
    }

    private List<Element> getLocations(String type, Document target) {
        Element locations = target.getRootElement().getChild("locations");
        if (locations != null) {
            return locations.getChildren().stream().filter(elem -> type.equals(elem.getAttributeValue("type")))
                    .toList();
        }
        return List.of();
    }

    @Override
    protected File getFileToBeUpdated() throws MojoFailureException {
        if (targetFile == null) {
            try {
                return TargetPlatformArtifactResolver.getMainTargetFile(project);
            } catch (TargetResolveException e) {
                throw new MojoFailureException(e);
            }
        } else {
            return targetFile;
        }
    }

    private static final class LatestVersionTarget implements TargetDefinition {

        private TargetDefinitionFile delegate;
        private TargetDefinitionVariableResolver varResolver;

        public LatestVersionTarget(TargetDefinitionFile delegate, TargetDefinitionVariableResolver varResolver) {
            this.delegate = delegate;
            this.varResolver = varResolver;
        }

        @Override
        public List<? extends Location> getLocations() {
            return delegate.getLocations().stream().map(location -> {
                if (location instanceof InstallableUnitLocation iuLocation) {
                    return new LatestVersionLocation(iuLocation, varResolver);
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
        private TargetDefinitionVariableResolver varResolver;

        public LatestVersionLocation(InstallableUnitLocation delegate, TargetDefinitionVariableResolver varResolver) {
            this.delegate = delegate;
            this.varResolver = varResolver;
        }

        @Override
        public List<? extends TargetDefinition.Repository> getRepositories() {
            return delegate.getRepositories().stream().map(repo -> {
                URI resolvedLocation = URI.create(varResolver.resolve(repo.getLocation()));
                return new ResolvedRepository(repo.getId(), resolvedLocation.toString());
            }).collect(toList());
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

        @Override
        public boolean includeConfigurePhase() {
            return delegate.includeConfigurePhase();
        }

        @Override
        public FollowRepositoryReferences followRepositoryReferences() {
            return delegate.followRepositoryReferences();
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

    private static final class ResolvedRepository implements TargetDefinition.Repository {

        private final String id;
        private final String uri;

        ResolvedRepository(String id, String uri) {
            this.id = id;
            this.uri = uri;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getLocation() {
            return uri;
        }

    }

}
