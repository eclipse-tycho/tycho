/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import static org.eclipse.tycho.versions.engine.Versions.isVersionEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.pom.DependencyManagement;
import org.eclipse.tycho.versions.pom.GAV;
import org.eclipse.tycho.versions.pom.MutablePomFile;
import org.eclipse.tycho.versions.pom.Plugin;
import org.eclipse.tycho.versions.pom.PluginManagement;

@Component(role = MetadataManipulator.class, hint = "pom")
public class PomManipulator extends AbstractMetadataManipulator {
    @Override
    public boolean addMoreChanges(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);
        GAV parent = pom.getParent();
        if (parent != null && isGavEquals(parent, change)
                && !isVersionEquals(change.getNewVersion(), parent.getVersion())) {
            String explicitVersion = pom.getVersion();
            if (explicitVersion == null || isVersionEquals(explicitVersion, change.getVersion())) {
                return allChanges.add(new VersionChange(pom, change.getVersion(), change.getNewVersion()));
            }
        }

        return false;
    }

    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);

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
        for (GAV dependency : pom.getDependencies()) {
            if (isGavEquals(dependency, change)) {
                logger.info("  pom.xml//project/dependencies/dependency/[ " + dependency.getGroupId() + ":"
                        + dependency.getArtifactId() + " ] " + version + " => " + newVersion);
                dependency.setVersion(newVersion);
            }
        }

        DependencyManagement dependencyManagment = pom.getDependencyManagement();

        if (dependencyManagment != null) {
            for (GAV dependency : dependencyManagment.getDependencies()) {
                if (isGavEquals(dependency, change)) {
                    logger.info("  pom.xml//project/dependencyManagement/dependencies/dependency/[ "
                            + dependency.getGroupId() + ":" + dependency.getArtifactId() + " ] " + version + " => "
                            + newVersion);
                    dependency.setVersion(newVersion);
                }
            }
        }

        applyChange("pom.xml//project/build/plugins/plugin", pom.getPlugins(), change, version, newVersion);

        PluginManagement pluginManagement = pom.getPluginManagement();
        if (pluginManagement != null) {
            applyChange("pom.xml//project/build/pluginManagemment/plugins/plugin", pluginManagement.getPlugins(),
                    change, version, newVersion);
        }

        // TODO update other references
    }

    private void applyChange(String pomPath, List<Plugin> plugins, VersionChange change, String version,
            String newVersion) {
        for (Plugin plugin : plugins) {
            GAV pluginGAV = plugin.getGAV();
            if (isGavEquals(pluginGAV, change)) {
                logger.info("  " + pomPath + "/[ " + pluginGAV.getGroupId() + ":" + pluginGAV.getArtifactId() + " ] "
                        + version + " => " + newVersion);
                pluginGAV.setVersion(newVersion);
            }

            for (GAV dependency : plugin.getDependencies()) {
                if (isGavEquals(dependency, change)) {
                    logger.info("  " + pomPath + "/[ " + pluginGAV.getGroupId() + ":" + pluginGAV.getArtifactId()
                            + " ] /dependencies/dependency/[ " + dependency.getGroupId() + ":"
                            + dependency.getArtifactId() + " ] " + version + " => " + newVersion);
                    dependency.setVersion(newVersion);
                }
            }
        }
    }

    private static boolean isGavEquals(MutablePomFile pom, VersionChange change) {
        // TODO replace with isGavEquals(pom.getEffectiveGav(), change)
        return change.getGroupId().equals(pom.getEffectiveGroupId())
                && change.getArtifactId().equals(pom.getArtifactId())
                && isVersionEquals(change.getVersion(), pom.getEffectiveVersion());
    }

    public static boolean isGavEquals(GAV gav, VersionChange change) {
        return change.getGroupId().equals(gav.getGroupId()) && change.getArtifactId().equals(gav.getArtifactId())
                && isVersionEquals(change.getVersion(), gav.getVersion());
    }

    public void writeMetadata(ProjectMetadata project) throws IOException {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);
        if (pom != null) {
            MutablePomFile.write(pom, new File(project.getBasedir(), "pom.xml"));
        }
    }

}
