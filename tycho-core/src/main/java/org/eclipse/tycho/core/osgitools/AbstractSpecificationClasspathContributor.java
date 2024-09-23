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
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.ClasspathEntry.AccessRule;
import org.eclipse.tycho.MavenArtifactKey;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.osgi.framework.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;

/**
 * Abstract implementation that contributes a "specification package" to the compile classpath in
 * two ways:
 * <ol>
 * <li>First the target platform is searched for a suitable artifact and this one is used</li>
 * <li>If nothing is found, a well known maven artifact is searched and used as an alternative</li>
 * </ol>
 */
public abstract class AbstractSpecificationClasspathContributor implements ClasspathContributor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final MavenBundleResolver mavenBundleResolver;

    protected final Provider<MavenSession> sessionProvider;

    protected final String packageName;

    protected final String mavenGroupId;

    protected final String mavenArtifactId;

    private final AccessRule accessRule;

    protected AbstractSpecificationClasspathContributor(MavenBundleResolver mavenBundleResolver,
                                                        Provider<MavenSession> sessionProvider,
                                                        String packageName,
                                                        String mavenGroupId,
                                                        String mavenArtifactId) {
        this.mavenBundleResolver = mavenBundleResolver;
        this.sessionProvider = sessionProvider;
        this.packageName = packageName;
        this.mavenGroupId = mavenGroupId;
        this.mavenArtifactId = mavenArtifactId;
        this.accessRule = new DefaultAccessRule(packageName.replace('.', '/') + "/*", false);
    }

    @Override
    public final List<ClasspathEntry> getAdditionalClasspathEntries(MavenProject project, String scope) {
        if (isValidProject(project)) {
            VersionRange specificationVersion = getSpecificationVersion(project);
            Optional<ResolvedArtifactKey> mavenBundle = findBundle(project, specificationVersion);
            if (mavenBundle.isPresent()) {
                ResolvedArtifactKey resolved = mavenBundle.get();
                logger.debug("Resolved " + packageName + " to " + resolved.getId() + " " + resolved.getVersion() + " @ "
                        + resolved.getLocation());
                return List.of(new DefaultClasspathEntry(resolved, List.of(accessRule)));
            }
            logger.warn("Cannot resolve specification package " + packageName + ", classpath might be incomplete");
        }
        return Collections.emptyList();
    }

    protected boolean isValidProject(MavenProject project) {
        return true;
    }

    protected Optional<ResolvedArtifactKey> findBundle(MavenProject project, VersionRange specificationVersion) {
        return mavenBundleResolver.resolveMavenBundle(project, sessionProvider.get(),
                MavenArtifactKey.of(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, packageName,
                        specificationVersion.toString(), mavenGroupId, mavenArtifactId));
    }

    protected abstract VersionRange getSpecificationVersion(MavenProject project);
}
