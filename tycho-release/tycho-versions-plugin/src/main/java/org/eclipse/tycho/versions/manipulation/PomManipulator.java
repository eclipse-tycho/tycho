/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - introduce VersionChangesDescriptor
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
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
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
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
        GAV parent = pom.getParent();

        boolean moreChanges = false;
        for (VersionChange change : versionChangeContext.getVersionChanges()) {
            if (parent != null && isGavEquals(parent, change)) {
                if (isVersionEquals(pom.getVersion(), change.getVersion())) {
                    moreChanges |= versionChangeContext
                            .addVersionChange(new VersionChange(pom, change.getVersion(), change.getNewVersion()));
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

        for (VersionChange change : versionChangeContext.getVersionChanges()) {
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
            VersionChange change, String version, String newVersion) {
        if (dependencyManagment != null) {
            changeDependencies(pomPath + "/dependencies", dependencyManagment.getDependencies(), change, version,
                    newVersion);
        }
    }

    protected void changeDependencies(String pomPath, List<GAV> dependencies, VersionChange change, String version,
            String newVersion) {
        for (GAV dependency : dependencies) {
            if (isGavEquals(dependency, change)) {
                logger.info(pomPath + "/dependency/[ " + dependency.getGroupId() + ":" + dependency.getArtifactId()
                        + " ] " + version + " => " + newVersion);
                dependency.setVersion(newVersion);
            }
        }
    }

    private void changeBuild(String pomPath, Build build, VersionChange change, String version, String newVersion) {
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

    private void changePlugins(String pomPath, List<Plugin> plugins, VersionChange change, String version,
            String newVersion) {
        for (Plugin plugin : plugins) {
            GAV pluginGAV = plugin.getGAV();
            if (isPluginGavEquals(pluginGAV, change)) {
                logger.info(pomPath + "/[ " + pluginGAV.getGroupId() + ":" + pluginGAV.getArtifactId() + " ] " + version
                        + " => " + newVersion);
                pluginGAV.setVersion(newVersion);
            }

            for (GAV dependency : plugin.getDependencies()) {
                if (isGavEquals(dependency, change)) {
                    logger.info(pomPath + "/[ " + pluginGAV.getGroupId() + ":" + pluginGAV.getArtifactId()
                            + " ] /dependencies/dependency/[ " + dependency.getGroupId() + ":"
                            + dependency.getArtifactId() + " ] " + version + " => " + newVersion);
                    dependency.setVersion(newVersion);
                }
            }
        }
    }

    private static boolean isGavEquals(PomFile pom, VersionChange change) {
        // TODO replace with isGavEquals(pom.getEffectiveGav(), change)
        return eq(change.getGroupId(), pom.getGroupId()) && eq(change.getArtifactId(), pom.getArtifactId())
                && isVersionEquals(change.getVersion(), pom.getVersion());
    }

    public static boolean isGavEquals(GAV gav, VersionChange change) {
        return eq(change.getGroupId(), gav.getGroupId()) && eq(change.getArtifactId(), gav.getArtifactId())
                && isVersionEquals(change.getVersion(), gav.getVersion());
    }

    public static boolean isPluginGavEquals(GAV gav, VersionChange change) {
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
