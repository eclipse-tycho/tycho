/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - inject nested class path elements into maven model (TYCHO-483)
 *    Christoph Läubrich - Issue #443 - Use regular Maven coordinates -when possible- for dependencies 
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.osgitools.BundleReader;

public final class MavenDependencyInjector {

    /* see RepositoryLayoutHelper#getP2Gav */
    private static final String P2_GROUPID_PREFIX = "p2.";

    /**
     * Injects the dependencies of a project (as determined by the p2 dependency resolver) back into
     * the Maven model.
     * 
     * @param project
     *            A project
     * @param dependencies
     *            The p2-resolved dependencies of the project.
     */
    public static void injectMavenDependencies(MavenProject project, DependencyArtifacts dependencies,
            DependencyArtifacts testDependencies, BundleReader bundleReader,
            Function<ArtifactDescriptor, MavenDependencyDescriptor> descriptorMapping, Logger logger) {
        MavenDependencyInjector generator = new MavenDependencyInjector(project, bundleReader, descriptorMapping,
                logger);
        for (ArtifactDescriptor artifact : dependencies.getArtifacts()) {
            generator.addDependency(artifact, Artifact.SCOPE_COMPILE);
        }
        if (testDependencies != null) {
            testDependencies.getArtifacts().stream() //
                    .filter(testDep -> dependencies.getArtifact(testDep.getKey()) == null) //
                    .forEach(descriptor -> generator.addDependency(descriptor, Artifact.SCOPE_TEST));
        }
    }

    private static final List<Dependency> NO_DEPENDENCIES = Collections.emptyList();

    private final BundleReader bundleReader;
    private final Logger logger;

    private final MavenProject project;

    private Function<ArtifactDescriptor, MavenDependencyDescriptor> descriptorMapping;

    MavenDependencyInjector(MavenProject project, BundleReader bundleReader,
            Function<ArtifactDescriptor, MavenDependencyDescriptor> descriptorMapping, Logger logger) {
        this.project = project;
        this.bundleReader = bundleReader;
        this.descriptorMapping = descriptorMapping;
        this.logger = logger;
    }

    void addDependency(ArtifactDescriptor artifact, String scope) {
        List<Dependency> dependencyList = new ArrayList<>();
        if (artifact.getMavenProject() != null) {
            dependencyList.addAll(newProjectDependencies(artifact));
        } else if (requiresExternalDependencies()) {
            dependencyList.addAll(newExternalDependencies(artifact));
        }
        Model model = project.getModel();
        for (Dependency dependency : dependencyList) {
            model.addDependency(dependency);
        }
    }

    private boolean requiresExternalDependencies() {
        return PackagingType.TYPE_ECLIPSE_PLUGIN.equals(project.getPackaging())
                || PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(project.getPackaging());
    }

    private List<Dependency> newExternalDependencies(ArtifactDescriptor artifact) {
        File location = artifact.getLocation(true);
        if (location == null || !location.isFile() || !location.canRead()) {
            logger.debug("Dependency at location " + location
                    + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins");
            return NO_DEPENDENCIES;
        }
        List<Dependency> result = new ArrayList<>();
        if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(artifact.getKey().getType())) {
            for (String classpathElement : getClasspathElements(location)) {
                if (".".equals(classpathElement)) {
                    result.add(createSystemScopeDependency(artifact, location));
                } else {
                    File nestedJarOrDir = bundleReader.getEntry(location, classpathElement);
                    if (nestedJarOrDir != null) {
                        if (nestedJarOrDir.isFile()) {
                            Dependency nestedJarDependency = createSystemScopeDependency(artifact, nestedJarOrDir);
                            nestedJarDependency.setClassifier(classpathElement);
                            result.add(nestedJarDependency);
                        } else if (nestedJarOrDir.isDirectory()) {
                            // system-scoped dependencies on directories are not supported
                            logger.debug("Dependency from " + project.getBasedir()
                                    + " to nested directory classpath entry " + nestedJarOrDir
                                    + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins");
                        }
                    }
                }
            }
        } else {
            result.add(createSystemScopeDependency(artifact, location));
        }
        return result;
    }

    private String[] getClasspathElements(File bundleLocation) {
        return bundleReader.loadManifest(bundleLocation).getBundleClasspath();
    }

    private Dependency createSystemScopeDependency(ArtifactDescriptor descriptor, File location) {
        ArtifactKey artifactKey = descriptor.getKey();
        return createSystemScopeDependency(descriptor, P2_GROUPID_PREFIX + artifactKey.getType(), location);
    }

    private Dependency createSystemScopeDependency(ArtifactDescriptor descriptor, String groupId, File location) {
        Dependency dependency = new Dependency();
        MavenDependencyDescriptor dependencyDescriptor = descriptorMapping == null ? null
                : descriptorMapping.apply(descriptor);
        if (dependencyDescriptor != null) {
            dependency.setGroupId(dependencyDescriptor.getGroupId());
            dependency.setArtifactId(dependencyDescriptor.getArtifactId());
            dependency.setVersion(dependencyDescriptor.getVersion());
            dependency.setClassifier(dependency.getClassifier());
            String type = dependencyDescriptor.getType();
            if (type != null && !type.isBlank()) {
                dependency.setType(type);
            }
            dependency.setScope(Artifact.SCOPE_SYSTEM);
            dependency.setSystemPath(location.getAbsolutePath());
        } else {
            ArtifactKey artifactKey = descriptor.getKey();
            dependency.setGroupId(groupId);
            dependency.setArtifactId(artifactKey.getId());
            dependency.setVersion(artifactKey.getVersion());
            dependency.setScope(Artifact.SCOPE_SYSTEM);
            dependency.setSystemPath(location.getAbsolutePath());
        }
        return dependency;
    }

    private List<Dependency> newProjectDependencies(ArtifactDescriptor artifact) {
        ReactorProject dependentMavenProjectProxy = artifact.getMavenProject();
        List<Dependency> result = new ArrayList<>();
        if (!artifact.getMavenProject().sameProject(project)) {
            result.add(createProvidedScopeDependency(dependentMavenProjectProxy));
        }
        // TODO treat eclipse-test-plugins in the same way?
        if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(dependentMavenProjectProxy.getPackaging())) {
            for (String classpathElement : getClasspathElements(dependentMavenProjectProxy.getBasedir())) {
                if (".".equals(classpathElement)) {
                    // covered by provided-scope dependency above
                    continue;
                } else /* nested classpath entry */
                {
                    File jar = new File(dependentMavenProjectProxy.getBasedir(), classpathElement);
                    // we can only add a system scope dependency for an existing (checked-in) jar file
                    // otherwise maven will throw a DependencyResolutionException
                    if (jar.isFile()) {
                        Dependency systemScopeDependency = createSystemScopeDependency(artifact,
                                artifact.getMavenProject().getGroupId(), jar);
                        systemScopeDependency.setClassifier(classpathElement);
                        result.add(systemScopeDependency);
                    } else {
                        logger.debug("Dependency from " + project.getBasedir() + " to nested classpath entry "
                                + jar.getAbsolutePath()
                                + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins");
                    }
                }
            }
        }
        return result;
    }

    private Dependency createProvidedScopeDependency(ReactorProject dependentReactorProject) {
        Dependency dependency = new Dependency();
        dependency.setArtifactId(dependentReactorProject.getArtifactId());
        dependency.setGroupId(dependentReactorProject.getGroupId());
        dependency.setVersion(dependentReactorProject.getVersion());
        dependency.setType(dependentReactorProject.getPackaging());
        dependency.setScope(Artifact.SCOPE_PROVIDED);
        return dependency;
    }
}
