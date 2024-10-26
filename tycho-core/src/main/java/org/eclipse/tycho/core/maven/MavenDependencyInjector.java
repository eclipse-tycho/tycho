/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - inject nested class path elements into maven model (TYCHO-483)
 *    Christoph Läubrich    - Issue #443 - Use regular Maven coordinates -when possible- for dependencies 
 *                          - Issue #581 - Use the correct scope when injecting dependencies into the maven model
 *                          - Issue #697 - Failed to resolve dependencies with Tycho 2.7.0 for custom repositories  
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.MavenArtifactRepositoryReference;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.slf4j.Logger;

public final class MavenDependencyInjector {

    /**
     * Injects a set of additional project dependencies into an existing maven project.
     * 
     * @param project
     * @param dependencyProjects
     */
    public static void injectMavenProjectDependencies(MavenProject project, Iterable<MavenProject> dependencyProjects) {
        Model model = project.getModel();
        Set<String> existingDependencies = model.getDependencies().stream().map(MavenDependencyInjector::getProjectKey)
                .collect(Collectors.toCollection(HashSet::new));
        for (MavenProject dependencyProject : dependencyProjects) {
            if (dependencyProject == project) {
                continue;
            }
            Dependency dependency = new Dependency();
            dependency.setArtifactId(dependencyProject.getArtifactId());
            dependency.setGroupId(dependencyProject.getGroupId());
            dependency.setVersion(dependencyProject.getVersion());
            String packaging = dependencyProject.getPackaging();
            dependency.setType(packaging);
            dependency.setScope(Artifact.SCOPE_COMPILE);
            dependency.setOptional(false);
            if (existingDependencies.add(getProjectKey(dependency))) {
                model.addDependency(dependency);
            }
        }
    }

    private static String getProjectKey(Dependency dependency) {

        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":"
                + Objects.requireNonNullElse(dependency.getType(), "jar") + ":" + dependency.getVersion() + ":"
                + Objects.requireNonNullElse(dependency.getClassifier(), "");
    }

    /**
     * Injects the dependencies of a project (as determined by the p2 dependency resolver) back into
     * the Maven model.
     * 
     * @param project
     *            A project
     * @param dependencies
     *            The p2-resolved dependencies of the project.
     * @param buildPropertiesParser
     */
    public static void injectMavenDependencies(MavenProject project, DependencyArtifacts dependencies,
            DependencyArtifacts testDependencies, BundleReader bundleReader,
            Function<ArtifactDescriptor, MavenDependencyDescriptor> descriptorMapping, Logger logger,
            RepositorySystem repositorySystem, Settings settings, BuildPropertiesParser buildPropertiesParser,
            TargetPlatformConfiguration configuration) {
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
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        BuildProperties buildProperties = buildPropertiesParser.parse(reactorProject);
        List<Dependency> extraJars = buildProperties.getJarsExtraClasspath().stream().map(extra -> {
            if (TychoConstants.PLATFORM_URL_PATTERN.matcher(extra).matches()) {
                //this should already be handled as an extra requirement!
                return null;
            }
            Dependency dependency = new Dependency();
            dependency.setScope(Artifact.SCOPE_SYSTEM);
            dependency.setGroupId(project.getGroupId());
            dependency.setArtifactId(project.getArtifactId() + ".jars.extra.classpath");
            dependency.setClassifier(extra);
            File file = new File(reactorProject.getBasedir(), extra);
            if (!file.exists()) {
                //create empty dummy file to make maven think this dependency is already resolved?!
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    //still can't be created --> out of luck then...
                    return null;
                }
            }
            dependency.setSystemPath(file.getAbsolutePath());
            dependency.setVersion(project.getVersion());
            return dependency;
        }).filter(Objects::nonNull).toList();
        generator.addDependencyList(extraJars);
        Collection<MavenArtifactRepositoryReference> repositoryReferences = configuration.getTargets().stream()
                .flatMap(definition -> definition.getLocations().stream()).filter(MavenGAVLocation.class::isInstance)
                .map(MavenGAVLocation.class::cast).flatMap(location -> location.getRepositoryReferences().stream())
                .toList();
        if (repositoryReferences != null && !repositoryReferences.isEmpty()) {
            Map<String, ArtifactRepository> repositoryMap = project.getRemoteArtifactRepositories().stream()
                    .collect(Collectors.toMap(MavenDependencyInjector::getId, Function.identity(), (a, b) -> a,
                            LinkedHashMap::new));
            for (MavenArtifactRepositoryReference reference : repositoryReferences) {
                String id = getId(reference);
                ArtifactRepository artifactRepository = repositoryMap.get(id);
                if (artifactRepository == null) {
                    repositoryMap.put(id,
                            repositorySystem.createArtifactRepository(id, reference.getUrl(), null, null, null));
                } else if (!artifactRepository.getUrl().equals(reference.getUrl())) {
                    logger.warn("Target defines an artifact repository with the ID " + id
                            + " but there is already a repository for that ID mapped to a different URL! (target URL = "
                            + reference.getUrl() + ", existing URL = " + artifactRepository.getUrl());
                }
            }
            List<ArtifactRepository> artifactRepositories = new ArrayList<>(repositoryMap.values());
            repositorySystem.injectMirror(artifactRepositories, settings.getMirrors());
            repositorySystem.injectProxy(artifactRepositories, settings.getProxies());
            repositorySystem.injectAuthentication(artifactRepositories, settings.getServers());
            project.setRemoteArtifactRepositories(artifactRepositories);
        }
    }

    private static String getId(MavenArtifactRepositoryReference reference) {
        String id = reference.getId();
        if (id == null || id.isBlank()) {
            return reference.getUrl();
        }
        return id;
    }

    private static String getId(ArtifactRepository repository) {
        String id = repository.getId();
        if (id == null || id.isBlank()) {
            return repository.getUrl();
        }
        return id;
    }

    private final BundleReader bundleReader;
    private final Logger logger;

    private final MavenProject project;
    private final boolean fetch;

    private Function<ArtifactDescriptor, MavenDependencyDescriptor> descriptorMapping;

    MavenDependencyInjector(MavenProject project, BundleReader bundleReader,
            Function<ArtifactDescriptor, MavenDependencyDescriptor> descriptorMapping, Logger logger) {
        this.project = project;
        this.fetch = PackagingType.TYPE_ECLIPSE_PLUGIN.equals(project.getPackaging())
                || PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(project.getPackaging());
        this.bundleReader = bundleReader;
        this.descriptorMapping = descriptorMapping;
        this.logger = logger;
    }

    void addDependency(ArtifactDescriptor artifact, String scope) {
        List<Dependency> dependencyList = artifact.getMavenProject() != null //
                ? collectProjectDependencies(artifact, scope) //
                : collectExternalDependencies(artifact, scope, true);
        addDependencyList(dependencyList);
        Map<String, MavenProject> projectReferences = project.getProjectReferences();
        ReactorProject mavenProject = artifact.getMavenProject();
        if (mavenProject != null && DefaultReactorProject.adapt(project) != mavenProject) {
            String key = new StringBuilder().append(mavenProject.getGroupId()).append(':')
                    .append(mavenProject.getArtifactId()).append(':').append(mavenProject.getVersion()).toString();
            if (!projectReferences.containsKey(key)) {
                logger.debug("Found a P2 dependency (" + artifact
                        + ") that is not reflected in the maven model project references");
            }
        }
    }

    private void addDependencyList(List<Dependency> dependencyList) {
        if (dependencyList.isEmpty()) {
            return;
        }
        Model model = project.getModel();
        Set<String> existing = model.getDependencies().stream().map(MavenDependencyInjector::getKey)
                .collect(Collectors.toCollection(HashSet::new));
        for (Dependency dependency : dependencyList) {
            if (existing.add(getKey(dependency))) {
                model.addDependency(dependency);
            }
        }
    }

    private static String getKey(Dependency dependency) {

        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getType() + ":"
                + dependency.getVersion() + ":" + Objects.requireNonNullElse(dependency.getClassifier(), "");
    }

    private List<Dependency> collectExternalDependencies(ArtifactDescriptor artifact, String scope,
            boolean retryFailed) {
        File location = artifact.getLocation(fetch);
        try {
            if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(artifact.getKey().getType())) {
                if (location == null || !location.isFile() || !location.canRead()) {
                    if (location != null && location.isDirectory()) {
                        logger.warn("Exploded plugin at location " + location
                                + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins.");
                        return Collections.emptyList();
                    } else {
                        Dependency p2Dependency = createP2Dependency(artifact, location, scope);
                        if (p2Dependency == null) {
                            logger.warn(artifact
                                    + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins.");
                            return Collections.emptyList();
                        }
                        return Collections.singletonList(p2Dependency);
                    }
                }
                List<Dependency> result = new ArrayList<>();
                for (String classpathElement : getClasspathElements(location)) {
                    if (".".equals(classpathElement)) {
                        result.add(createP2Dependency(artifact, location, scope));
                    } else {
                        File nestedJarOrDir = bundleReader.getEntry(location, classpathElement);
                        if (nestedJarOrDir != null) {
                            if (nestedJarOrDir.isFile()) {
                                Dependency nestedJarDependency = createP2Dependency(artifact, nestedJarOrDir,
                                        Artifact.SCOPE_SYSTEM);
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
                return result;
            } else {
                return Collections.singletonList(createP2Dependency(artifact, location, scope));
            }
        } catch (OsgiManifestParserException e) {
            if (location != null && retryFailed && location.isFile()) {
                if (e.getCause() instanceof ZipException) {
                    logger.warn("Artifact " + artifact + " located at " + location
                            + " seems corrupted! Will attempt to download it again");
                    location.delete();
                    return collectExternalDependencies(artifact, scope, false);
                }
            }
            throw e;
        }
    }

    private String[] getClasspathElements(File bundleLocation) {
        return bundleReader.loadManifest(bundleLocation).getBundleClasspath();
    }

    private Dependency createP2Dependency(ArtifactDescriptor descriptor, File location, String scope) {
        ArtifactKey artifactKey = descriptor.getKey();
        return createScopedDependency(descriptor,
                TychoConstants.P2_GROUPID_PREFIX + artifactKey.getType().replace('-', '.'), location, scope);
    }

    private Dependency createScopedDependency(ArtifactDescriptor descriptor, String groupId, File location,
            String scope) {
        Dependency dependency = new ArtifactDescriptorDependency(descriptor);
        MavenDependencyDescriptor dependencyDescriptor = descriptorMapping == null ? null
                : descriptorMapping.apply(descriptor);
        if (dependencyDescriptor != null && isValidMavenDescriptor(dependencyDescriptor)) {
            dependency.setGroupId(dependencyDescriptor.getGroupId());
            dependency.setArtifactId(dependencyDescriptor.getArtifactId());
            dependency.setVersion(dependencyDescriptor.getVersion());
            dependency.setClassifier(dependency.getClassifier());
            String type = dependencyDescriptor.getType();
            if (type != null && !type.isBlank()) {
                dependency.setType(type);
            } else {
                dependency.setType(descriptor.getKey().getType());
            }
            dependency.setScope(scope);
        } else {
            ArtifactKey artifactKey = descriptor.getKey();
            dependency.setGroupId(groupId);
            dependency.setArtifactId(artifactKey.getId());
            dependency.setVersion(artifactKey.getVersion());
            dependency.setScope(Artifact.SCOPE_SYSTEM);
            dependency.setType(artifactKey.getType());
        }
        if (Artifact.SCOPE_SYSTEM.equals(dependency.getScope())) {
            dependency.setSystemPath(location != null && location.isFile() //
                    ? location.getAbsolutePath() //
                    : fileNotAvailable());
        }
        return dependency;
    }

    public static boolean isValidMavenDescriptor(MavenDependencyDescriptor dependencyDescriptor) {
        if (dependencyDescriptor == null) {
            return false;
        }
        //TODO we should make this configurable maybe on the tycho plugin level e.g.
        //        <plugin>
        //            <groupId>org.eclipse.tycho</groupId>
        //            <artifactId>tycho-maven-plugin</artifactId>
        //            <version>${tycho-version}</version>
        //            <extensions>true</extensions>
        //            <configuration>
        //              <mavenDescriptorRepositories>
        //                  <repository>central</repository>
        //                  <repository>!eclipse-snapshots</repository> 
        //                  <repository>....</repository> 
        //              </mavenDescriptorRepositories>       
        //            </configuration>
        //        </plugin>
        String repository = dependencyDescriptor.getRepository();
        return repository != null && !repository.isBlank();
    }

    private List<Dependency> collectProjectDependencies(ArtifactDescriptor artifact, String scope) {
        ReactorProject dependentMavenProjectProxy = artifact.getMavenProject();
        List<Dependency> result = new ArrayList<>();
        if (!artifact.getMavenProject().sameProject(project)) {
            result.add(createProjectDependency(artifact, dependentMavenProjectProxy, scope));
        }
        if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(dependentMavenProjectProxy.getPackaging())
                || PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(dependentMavenProjectProxy.getPackaging())) {
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
                        Dependency systemScopeDependency = createScopedDependency(artifact,
                                artifact.getMavenProject().getGroupId(), jar, Artifact.SCOPE_SYSTEM);
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

    private Dependency createProjectDependency(ArtifactDescriptor artifact, ReactorProject dependentReactorProject,
            String scope) {
        Dependency dependency = new ArtifactDescriptorDependency(artifact);
        dependency.setArtifactId(dependentReactorProject.getArtifactId());
        dependency.setGroupId(dependentReactorProject.getGroupId());
        dependency.setVersion(dependentReactorProject.getVersion());
        String type = dependentReactorProject.getPackaging();
        dependency.setType(type);
        dependency.setScope(scope);
        return dependency;
    }

    private static final class ArtifactDescriptorDependency extends Dependency implements ArtifactDescriptor {

        private static final long serialVersionUID = 1L;
        private ArtifactDescriptor descriptor;

        public ArtifactDescriptorDependency(ArtifactDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public String toString() {
            return "ArtifactDescriptorDependency {descriptor=" + getDescriptor() + ", groupId=" + getGroupId()
                    + ", artifactId=" + getArtifactId() + ", version=" + getVersion() + ", type=" + getType()
                    + ", classifier=" + getClassifier() + "}";
        }

        public ArtifactDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public ArtifactKey getKey() {
            return getDescriptor().getKey();
        }

        @Override
        public File getLocation(boolean fetch) {
            return getDescriptor().getLocation(fetch);
        }

        @Override
        public ReactorProject getMavenProject() {
            return getDescriptor().getMavenProject();
        }

        @Override
        public Collection<IInstallableUnit> getInstallableUnits() {
            return getDescriptor().getInstallableUnits();
        }

        @Override
        public Dependency clone() {

            ArtifactDescriptorDependency copy = new ArtifactDescriptorDependency(descriptor);
            copy.setArtifactId(getArtifactId());
            copy.setClassifier(getClassifier());
            copy.setExclusions(new ArrayList<>(getExclusions()));
            copy.setGroupId(getGroupId());
            if (copy.getOptional() != null) {
                copy.setOptional(isOptional());
            }
            copy.setScope(getScope());
            copy.setSystemPath(getSystemPath());
            copy.setType(getType());
            copy.setVersion(getVersion());
            return copy;
        }

        @Override
        public Optional<File> getLocation() {
            return getDescriptor().getLocation();
        }

        @Override
        public CompletableFuture<File> fetchArtifact() {
            return getDescriptor().fetchArtifact();
        }

    }

    private static File fileNotYetAvailable;

    private static String fileNotAvailable() {
        if (fileNotYetAvailable == null) {
            try {
                fileNotYetAvailable = File.createTempFile("file not yet available", null);
                fileNotYetAvailable.deleteOnExit();
                try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(fileNotYetAvailable),
                        new Manifest())) {
                    //create an empty jar just in case...
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return fileNotYetAvailable.getAbsolutePath();
    }
}
