/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.Wagon;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.api.DefaultVersionsHelper;
import org.codehaus.mojo.versions.api.VersionRetrievalException;
import org.codehaus.mojo.versions.api.VersionsHelper;
import org.codehaus.mojo.versions.utils.ArtifactFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;

import de.pdark.decentxml.Element;

/**
 * Supports updating of maven target locations
 */
@Named
public class MavenLocationUpdater {

    @Inject
    protected ArtifactFactory artifactFactory;

    @Inject
    protected RepositorySystem repositorySystem;
    @Inject
    protected Map<String, Wagon> wagonMap;

    List<MavenVersionUpdate> update(Element mavenLocation, UpdateTargetMojo context)
            throws VersionRangeResolutionException, ArtifactResolutionException, MojoExecutionException,
            VersionRetrievalException {
        VersionsHelper helper = getHelper(context);
        Element dependencies = mavenLocation.getChild("dependencies");
        List<MavenVersionUpdate> updates = new ArrayList<>();
        if (dependencies != null) {
            for (Element dependency : dependencies.getChildren("dependency")) {
                Dependency mavenDependency = getDependency(dependency);
                Artifact dependencyArtifact = artifactFactory.createArtifact(mavenDependency);
                ArtifactVersions versions = helper.lookupArtifactVersions(dependencyArtifact, false);
                ArtifactVersion updateVersion = context.getSegments()
                        .map(seg -> versions.getNewestUpdateWithinSegment(Optional.of(seg), false))
                        .filter(Objects::nonNull).findFirst().orElse(null);
                if (updateVersion != null) {
                    String oldVersion = mavenDependency.getVersion();
                    String newVersion = updateVersion.toString();
                    if (newVersion.equals(oldVersion)) {
                        context.getLog().debug(mavenDependency + " is already up-to date");
                    } else {
                        UpdateTargetMojo.setElementValue("version", newVersion, dependency);
                        context.getLog().info("update " + mavenDependency + " to version " + newVersion);
                        IInstallableUnit current = getIU(helper, dependencyArtifact);
                        Dependency clone = mavenDependency.clone();
                        clone.setVersion(newVersion);
                        IInstallableUnit update = getIU(helper, artifactFactory.createArtifact(clone));
                        updates.add(new MavenVersionUpdate(dependencyArtifact, newVersion, current, update));
                    }
                }
            }
        }
        return updates;
    }

    private IInstallableUnit getIU(VersionsHelper helper, Artifact dependencyArtifact) {
        try {
            helper.resolveArtifact(dependencyArtifact, false);
            File file = dependencyArtifact.getFile();
            BundleDescription bundleDescription = BundlesAction.createBundleDescription(file);
            if (bundleDescription != null) {
                IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(),
                        bundleDescription.getVersion().toString());
                PublisherInfo publisherInfo = new PublisherInfo();
                publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
                return BundlesAction.createBundleIU(bundleDescription, key, publisherInfo);
            }
        } catch (Exception e) {
        }
        return null;
    }

    VersionsHelper getHelper(UpdateTargetMojo context) throws MojoExecutionException {
        return new DefaultVersionsHelper.Builder().withRepositorySystem(repositorySystem).withLog(context.getLog())
                .withMavenSession(context.getMavenSession()).build();
    }

    private static Dependency getDependency(Element dependency) {
        Dependency mavenDependency = new Dependency();
        mavenDependency.setGroupId(UpdateTargetMojo.getElementValue("groupId", dependency));
        mavenDependency.setArtifactId(UpdateTargetMojo.getElementValue("artifactId", dependency));
        mavenDependency.setVersion(UpdateTargetMojo.getElementValue("version", dependency));
        mavenDependency.setType(UpdateTargetMojo.getElementValue("type", dependency));
        mavenDependency.setClassifier(UpdateTargetMojo.getElementValue("classifier", dependency));
        if (mavenDependency.getType() == null) {
            mavenDependency.setType(TychoConstants.JAR_EXTENSION);
        }
        return mavenDependency;
    }

}
