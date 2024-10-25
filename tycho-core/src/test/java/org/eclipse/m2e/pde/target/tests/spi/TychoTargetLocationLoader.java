/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests.spi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.tycho.core.resolver.MavenTargetLocationFactory;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.target.FileArtifactRepository;
import org.eclipse.tycho.targetplatform.TargetDefinition.Location;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.eclipse.tycho.testing.TychoPlexusTestCase;

public class TychoTargetLocationLoader implements TargetLocationLoader {

    @Override
    public int getPriority() {
        // use a higher priority to override m2e provided loader...
        return 100;
    }

    @Override
    public ITargetLocation resolveMavenTarget(String targetXML, File tempDir) throws Exception {
        File targetFile = new File(tempDir, "test.target");
        try (BufferedWriter writer = Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8)) {
            writer.write("<target name=\"test-target-platform\"><locations>");
            writer.write(targetXML);
            writer.write("</locations></target>");
        }
        PlexusContainer container = new ContainerFactory().createContainer(tempDir);
        MavenTargetLocationFactory locationFactory = container.lookup(MavenTargetLocationFactory.class);
        TargetDefinitionFile targetDefinition = TargetDefinitionFile.read(targetFile);
        for (Location loc : targetDefinition.getLocations()) {
            if (loc instanceof MavenGAVLocation gav) {
                File locationTempDir = new File(tempDir, "tmp");
                locationTempDir.mkdirs();
                return getLocationOf(gav, locationFactory, locationTempDir.toPath());
            }
        }
        throw new IllegalArgumentException("Can't extract MavenGAVLocation from " + targetXML);
    }

    private ITargetLocation getLocationOf(MavenGAVLocation gav, MavenTargetLocationFactory locationFactory,
            Path tmpFir) {
        try {
            return getLocationOf(locationFactory.resolveTargetDefinitionContent(gav, IncludeSourceMode.honor), tmpFir);
        } catch (Exception e) {
            return new ITargetLocation() {

                @Override
                public <T> T getAdapter(Class<T> adapter) {
                    return null;
                }

                @Override
                public String serialize() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public String[] getVMArguments() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getType() {
                    return MAVEN_LOCATION_TYPE;
                }

                @Override
                public IStatus getStatus() {
                    return Status.error("Resolve target location failed: " + e, e);
                }

                @Override
                public String getLocation(boolean resolve) throws CoreException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public TargetFeature[] getFeatures() {
                    return new TargetFeature[0];
                }

                @Override
                public TargetBundle[] getBundles() {
                    return new TargetBundle[0];
                }
            };
        }
    }

    private ITargetLocation getLocationOf(TargetDefinitionContent content, Path tmpFir) throws Exception {
        List<TargetBundle> bundles = new ArrayList<>();
        List<TargetFeature> features = new ArrayList<>();
        FileArtifactRepository resolvedItems = (FileArtifactRepository) content.getArtifactRepository();
        IQuery<IArtifactDescriptor> query = new IQuery<>() {

            @Override
            public IQueryResult<IArtifactDescriptor> perform(Iterator<IArtifactDescriptor> iterator) {
                Collector<IArtifactDescriptor> result = new Collector<>();
                while (iterator.hasNext()) {
                    IArtifactDescriptor candidate = iterator.next();
                    if (candidate != null)
                        if (!result.accept(candidate))
                            break;
                }
                return result;
            }

            @Override
            public IExpression getExpression() {
                return null;
            }
        };
        Set<IArtifactDescriptor> descriptors = resolvedItems.descriptorQueryable().query(query, null).toSet();
        for (IArtifactDescriptor descriptor : descriptors) {
            File artifactFile = resolvedItems.getArtifactFile(descriptor);
            if ("osgi.bundle".equals(descriptor.getArtifactKey().getClassifier())) {
                bundles.add(new TargetBundle(artifactFile));
            } else if ("org.eclipse.update.feature".equals(descriptor.getArtifactKey().getClassifier())) {
                Path feature;
                try (JarFile file = new JarFile(artifactFile)) {
                    Path tempDirectory = Files.createTempDirectory(tmpFir, "extract");
                    ZipEntry entry = file.getEntry("feature.xml");
                    InputStream stream = file.getInputStream(entry);
                    feature = tempDirectory.resolve("feature.xml");
                    Files.copy(stream, feature);
                }
                features.add(new TargetFeature(feature.toFile()));
            }
        }
        return new ITargetLocation() {

            @Override
            public <T> T getAdapter(Class<T> adapter) {
                return null;
            }

            @Override
            public String serialize() {
                throw new UnsupportedOperationException();
            }

            @Override
            public IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isResolved() {
                return true;
            }

            @Override
            public String[] getVMArguments() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getType() {
                return MAVEN_LOCATION_TYPE;
            }

            @Override
            public IStatus getStatus() {
                return Status.OK_STATUS;
            }

            @Override
            public String getLocation(boolean resolve) throws CoreException {
                throw new UnsupportedOperationException();
            }

            @Override
            public TargetFeature[] getFeatures() {
                return features.toArray(TargetFeature[]::new);
            }

            @Override
            public TargetBundle[] getBundles() {
                return bundles.toArray(TargetBundle[]::new);
            }
        };
    }

    private static final class ContainerFactory extends TychoPlexusTestCase {

        private boolean init;

        public PlexusContainer createContainer(File tempDir) throws ComponentLookupException {
            PlexusContainer container = super.getContainer();
            if (!init) {
                init = true;
                LegacySupport legacySupport = lookup(LegacySupport.class);
//                Settings settings = new Settings();
                ArtifactRepository localRepository = new StubArtifactRepository(
                        new File(tempDir, ".m2/repository").getAbsolutePath()) {
                    DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

                    @Override
                    public String pathOf(Artifact artifact) {
                        return layout.pathOf(artifact);
                    }
                };
                DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
                request.setUserProperties(System.getProperties());
                request.setLocalRepository(localRepository);
                request.setGoals(List.of());
                request.setBaseDirectory(new File(tempDir, "build"));
                request.setStartTime(new Date());

                RepositorySystemSession repositorySystemSession = LegacyLocalRepositoryManager.overlay(localRepository,
                        MavenRepositorySystemUtils.newSession(), null);
                MavenSession mavenSession = new MavenSession(container, repositorySystemSession, request,
                        new DefaultMavenExecutionResult());
//                MavenSession mavenSession = new MavenSession(container, settings, localRepository, null, null,
//                        List.of(), new File(tempDir, "build").getAbsolutePath(), System.getProperties(),
//                        System.getProperties(), new Date());
                SessionScope sessionScope = container.lookup(SessionScope.class);
                mavenSession.setProjects(Collections.emptyList());
                sessionScope.enter();
                sessionScope.seed(MavenSession.class, mavenSession);
                legacySupport.setSession(mavenSession);
            }
            return container;
        }
    }

}
