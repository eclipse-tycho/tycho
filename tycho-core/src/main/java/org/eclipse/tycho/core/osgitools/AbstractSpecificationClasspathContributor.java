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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.ClasspathEntry.AccessRule;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Abstract implementation that contributes a "specification package" to the compile classpath in
 * two ways:
 * <ol>
 * <li>First the target platform is searched for a suitable artifact and this one is used</li>
 * <li>If nothing is found, a well known maven artifact is searched and used as an alternative</li>
 * </ol>
 */
public abstract class AbstractSpecificationClasspathContributor implements ClasspathContributor {
    @Requirement
    private MavenDependenciesResolver dependenciesResolver;

    @Requirement
    protected Logger logger;

    @Requirement
    private TychoProjectManager projectManager;

    protected final MavenSession session;

    protected final String packageName;

    protected final String mavenGroupId;

    protected final String mavenArtifactId;

    private final AccessRule accessRule;

    protected AbstractSpecificationClasspathContributor(MavenSession session, String packageName, String mavenGroupId,
            String mavenArtifactId) {
        this.session = session;
        this.packageName = packageName;
        this.mavenGroupId = mavenGroupId;
        this.mavenArtifactId = mavenArtifactId;
        this.accessRule = new DefaultAccessRule(packageName.replace('.', '/') + "/*", false);

    }

    @Override
    public final List<ClasspathEntry> getAdditionalClasspathEntries(ReactorProject project, String scope) {
        Version specificationVersion = getSpecificationVersion(project);
        Version nextMajor = new Version(specificationVersion.getMajor() + 1, 0, 0);
        TargetPlatform tp = TychoProjectUtils.getTargetPlatformIfAvailable(project);
        // try to resolve from TP first...
        if (tp != null) {
            try {
                VersionRange versionRange = new VersionRange(VersionRange.LEFT_CLOSED, specificationVersion, nextMajor,
                        VersionRange.RIGHT_OPEN);
                ResolvedArtifactKey resolvePackage = tp.resolvePackage(packageName, versionRange.toString());
                logger.debug("Resolved " + packageName + " to " + resolvePackage.getId() + " "
                        + resolvePackage.getVersion() + " @ " + resolvePackage.getLocation());
                return List.of(new DefaultClasspathEntry(resolvePackage, List.of(accessRule)));
            } catch (DependencyResolutionException | IllegalArtifactReferenceException e) {
                logger.debug("Cannot find package " + packageName + " in target platform: " + e);
            }
        }
        if (mavenGroupId != null && mavenArtifactId != null) {
            try {
                // then fallback to maven artifact ...
                Dependency dependency = new Dependency();
                dependency.setGroupId(mavenGroupId);
                dependency.setArtifactId(mavenArtifactId);
                dependency.setVersion(String.format("[%d.%d,%d)", specificationVersion.getMajor(),
                        specificationVersion.getMinor(), nextMajor.getMajor()));
                Artifact artifact = dependenciesResolver.resolveHighestVersion(project.adapt(MavenProject.class),
                        session, dependency);
                ArtifactKey artifactKey = projectManager.getArtifactKey(artifact);
                logger.debug("Resolved " + packageName + " to " + artifact.getId() + " @ " + artifact.getFile());
                return List.of(
                        new DefaultClasspathEntry(null, artifactKey, List.of(artifact.getFile()), List.of(accessRule)));
            } catch (VersionRangeResolutionException | ArtifactResolutionException e) {
                logger.debug("Can't find maven artifact " + mavenGroupId + ":" + mavenArtifactId + " for package "
                        + packageName + ": " + e);
            }
        }
        logger.warn("Can't resolve specification package " + packageName
                + " neither from the target platform nor from maven artifacts, classpath might be incomplete!");
        return Collections.emptyList();
    }

    protected abstract Version getSpecificationVersion(ReactorProject project);
}
