/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - introduce VersionChangesDescriptor
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *    Christoph Läubrich - Bug 550313 - tycho-versions-plugin uses hard-coded polyglot file 
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import static org.eclipse.tycho.versions.engine.Versions.eq;
import static org.eclipse.tycho.versions.engine.Versions.isVersionEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.PomVersionChange;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.pom.Build;
import org.eclipse.tycho.versions.pom.DependencyManagement;
import org.eclipse.tycho.versions.pom.GAV;
import org.eclipse.tycho.versions.pom.Plugin;
import org.eclipse.tycho.versions.pom.PluginManagement;
import org.eclipse.tycho.versions.pom.PomFile;
import org.eclipse.tycho.versions.pom.Profile;
import org.eclipse.tycho.versions.pom.Property;

@Component(role = MetadataManipulator.class, hint = PomManipulator.HINT)
public class PomManipulator extends AbstractMetadataManipulator {
    public static final String HINT = "pom";

    @Override
    public boolean addMoreChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        PomFile pom = project.getMetadata(PomFile.class);
        if (pom == null) {
            throw new RuntimeException("no pom avaiable for " + project.getBasedir());
        }
        GAV parent = pom.getParent();

        boolean moreChanges = false;
        for (PomVersionChange change : versionChangeContext.getVersionChanges()) {
            if (parent != null && isGavEquals(parent, change)) {
                if (isVersionEquals(pom.getVersion(), change.getVersion())) {
                    moreChanges |= versionChangeContext
                            .addVersionChange(new PomVersionChange(pom, change.getVersion(), change.getNewVersion()));
                }
            }
        }
        return moreChanges;
    }

    @Override
    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        PomFile pom = project.getMetadata(PomFile.class);
        // only do the real change if the pom file is mutable
        // e.g. not for polyglot pom files
        if (!pom.isMutable()) {
            return;
        }
        // TODO visitor pattern is a better way to implement this

        for (PomVersionChange change : versionChangeContext.getVersionChanges()) {
            String version = Versions.toMavenVersion(change.getVersion());
            String newVersion = Versions.toMavenVersion(change.getNewVersion());
            if (isGavEquals(pom, change)) {
                logger.info("  pom.xml//project/version: " + version + " => " + newVersion);
                pom.setVersion(newVersion);
            } else {
                GAV parent = pom.getParent();
                if (parent != null && isGavEquals(parent, change)) {
                    logger.info("  pom.xml//project/parent/version: " + version + " => " + newVersion);
                    parent.setVersion(newVersion);
                }
            }

            //
            // Dependencies and entries inside dependencyManagement sections are not
            // OSGI related. Nevertheless it might happen that dependencies like this
            // does occur inside OSGI related project. Hence we must be able to handle
            // it.
            //

            changeDependencies("  pom.xml//project/dependencies", pom.getDependencies(), change, version, newVersion);
            changeDependencyManagement("  pom.xml//project/dependencyManagement", pom.getDependencyManagement(), change,
                    version, newVersion);

            changeBuild("  pom.xml//project/build", pom.getBuild(), change, version, newVersion);

            for (Profile profile : pom.getProfiles()) {
                String profileId = profile.getId() != null ? profile.getId() : "<null>";
                String pomPath = "  pom.xml//project/profiles/profile[ " + profileId + " ]";
                changeDependencies(pomPath + "/dependencies", profile.getDependencies(), change, version, newVersion);
                changeDependencyManagement(pomPath + "/dependencyManagement", profile.getDependencyManagement(), change,
                        version, newVersion);
                changeBuild(pomPath + "/build", profile.getBuild(), change, version, newVersion);
            }
        }

    }

    protected void changeDependencyManagement(String pomPath, DependencyManagement dependencyManagment,
            PomVersionChange change, String version, String newVersion) {
        if (dependencyManagment != null) {
            changeDependencies(pomPath + "/dependencies", dependencyManagment.getDependencies(), change, version,
                    newVersion);
        }
    }

    protected void changeDependencies(String pomPath, List<GAV> dependencies, PomVersionChange change, String version,
            String newVersion) {
        for (GAV dependency : dependencies) {
            if (isGavEquals(dependency, change)) {
                logger.info(pomPath + "/dependency/[ " + dependency.getGroupId() + ":" + dependency.getArtifactId()
                        + " ] " + version + " => " + newVersion);
                dependency.setVersion(newVersion);
            }
        }
    }

    private void changeBuild(String pomPath, Build build, PomVersionChange change, String version, String newVersion) {
        if (build == null) {
            return;
        }
        changePlugins(pomPath + "/plugins/plugin", build.getPlugins(), change, version, newVersion);
        PluginManagement pluginManagement = build.getPluginManagement();
        if (pluginManagement != null) {
            changePlugins(pomPath + "/pluginManagemment/plugins/plugin", pluginManagement.getPlugins(), change, version,
                    newVersion);
        }
    }

    private void changePlugins(String pomPath, List<Plugin> plugins, PomVersionChange change, String version,
            String newVersion) {
        for (Plugin plugin : plugins) {
            GAV pluginGAV = plugin.getGAV();
            if (isPluginGavEquals(pluginGAV, change)) {
                logger.info(pomPath + "/[ " + pluginGAV.getGroupId() + ":" + pluginGAV.getArtifactId() + " ] " + version
                        + " => " + newVersion);
                pluginGAV.setVersion(newVersion);
            }

            changePlugins(pomPath, pluginGAV, change, version, newVersion, "/dependencies/dependency/",
                    plugin.getDependencies());
            changePlugins(pomPath, pluginGAV, change, version, newVersion, "/configuration/target/artifact/",
                    plugin.getTargetArtifacts());

        }
    }

    // change version of list of GAV in a plugin
    private void changePlugins(String pomPath, GAV pluginGAV, PomVersionChange change, String version,
            String newVersion, String subPath, List<GAV> gavs) {
        for (GAV targetArtifact : gavs) {
            if (isGavEquals(targetArtifact, change)) {
                logger.info(pomPath + "/[ " + pluginGAV.getGroupId() + ":" + pluginGAV.getArtifactId() + " ] " + subPath
                        + "[ " + targetArtifact.getGroupId() + ":" + targetArtifact.getArtifactId() + " ] " + version
                        + " => " + newVersion);
                targetArtifact.setVersion(newVersion);
            }
        }
    }

    private static boolean isGavEquals(PomFile pom, PomVersionChange change) {
        // TODO replace with isGavEquals(pom.getEffectiveGav(), change)
        return eq(change.getGroupId(), pom.getGroupId()) && eq(change.getArtifactId(), pom.getArtifactId())
                && isVersionEquals(change.getVersion(), pom.getVersion());
    }

    public static boolean isGavEquals(GAV gav, PomVersionChange change) {
        return eq(change.getGroupId(), gav.getGroupId()) && eq(change.getArtifactId(), gav.getArtifactId())
                && isVersionEquals(change.getVersion(), gav.getVersion());
    }

    public static boolean isPluginGavEquals(GAV gav, PomVersionChange change) {
        String groupId = gav.getGroupId() != null ? gav.getGroupId() : "org.apache.maven.plugins";
        return eq(change.getGroupId(), groupId) && eq(change.getArtifactId(), gav.getArtifactId())
                && isVersionEquals(change.getVersion(), gav.getVersion());
    }

    @Override
    public void writeMetadata(ProjectMetadata project) throws IOException {
        PomFile pom = project.getMetadata(PomFile.class);
        File pomFile = new File(project.getBasedir(), "pom.xml");
        if (pom != null && pomFile.exists()) {
            PomFile.write(pom, pomFile);
        }
    }

    public void applyPropertyChange(PomFile pom, String propertyName, String propertyValue) {
        changeProperties("  pom.xml//project/properties", pom.getProperties(), propertyName, propertyValue);
        for (Profile profile : pom.getProfiles()) {
            String pomPath = "  pom.xml//project/profiles/profile[ " + profile.getId() + " ]/properties";
            changeProperties(pomPath, profile.getProperties(), propertyName, propertyValue);
        }
    }

    private void changeProperties(String pomPath, List<Property> properties, String propertyName,
            String propertyValue) {
        for (Property property : properties) {
            if (propertyName.equals(property.getName())) {
                logger.info(pomPath + "/[ " + propertyName + " ] " + property.getValue() + " => " + propertyValue);
                property.setValue(propertyValue);
            }
        }
    }

    @Override
    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        return null; // there are no restrictions on maven version format
    }
}
