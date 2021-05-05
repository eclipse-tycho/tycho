/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.launching.internal.P2ApplicationLauncher;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

@Mojo(name = "maven-p2-site", requiresDependencyResolution = ResolutionScope.COMPILE)
public class MavenP2SiteMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Component
    private EquinoxServiceFactory equinox;

    @Component
    private Logger logger;

    @Parameter(defaultValue = "compile")
    private String dependencyScope;

    @Parameter(defaultValue = "300")
    private int timeoutInSeconds = 300;

    @Parameter(defaultValue = "Bundles")
    private String categoryName;

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private P2ApplicationLauncher launcher;

    @Parameter(defaultValue = "${project.build.directory}/repository")
    private File destination;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Dependency> dependencies = project.getDependencies();
        List<File> bundles = new ArrayList<>();
        List<File> advices = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            logger.debug("resolved " + dependency.getGroupId() + "::" + dependency.getArtifactId() + "::"
                    + dependency.getVersion() + "::" + dependency.getClassifier());
            Artifact artifact = repositorySystem.createArtifactWithClassifier(dependency.getGroupId(),
                    dependency.getArtifactId(), dependency.getVersion(), dependency.getType(),
                    dependency.getClassifier());
            Set<Artifact> artifacts = resolveArtifact(artifact, dependencyScope);
            for (Artifact resolvedArtifact : artifacts) {
                bundles.add(resolvedArtifact.getFile());
                try {
                    int cnt = 0;
                    File p2 = File.createTempFile("p2properties", ".inf");
                    p2.deleteOnExit();
                    Properties properties = new Properties();
                    addProperty(properties, RepositoryLayoutHelper.PROP_GROUP_ID, resolvedArtifact.getGroupId(), cnt++);
                    addProperty(properties, RepositoryLayoutHelper.PROP_ARTIFACT_ID, resolvedArtifact.getArtifactId(),
                            cnt++);
                    addProperty(properties, RepositoryLayoutHelper.PROP_VERSION, resolvedArtifact.getVersion(), cnt++);
                    addProperty(properties, RepositoryLayoutHelper.PROP_CLASSIFIER, resolvedArtifact.getClassifier(),
                            cnt++);
                    addProperty(properties, RepositoryLayoutHelper.PROP_EXTENSION, resolvedArtifact.getType(), cnt++);
                    //TODO pgp.signatures --> getSignatureFile(resolvedArtifact)
                    properties.store(new FileOutputStream(p2), null);
                    advices.add(p2);
                } catch (IOException e) {
                    throw new MojoExecutionException("failed to generate p2.inf", e);
                }
            }
        }
        File category;
        try {
            category = File.createTempFile("category", ".xml");

            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(category), StandardCharsets.UTF_8))) {
                writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                writer.println("<site>");
                writer.println("<category-def name=\"bundles\" label=\"" + categoryName + "\"/>");
                writer.println("<iu>");
                writer.println("<category name=\"bundles\"/>");
                writer.println(
                        " <query><expression type=\"match\">providedCapabilities.exists(p | p.namespace == 'osgi.bundle')</expression></query>");
                writer.println("</iu>");
                writer.println("</site>");
            }
            category.deleteOnExit();
        } catch (IOException e) {
            throw new MojoExecutionException("failed to generate category.xml", e);
        }

        destination.mkdirs();
        launcher.setWorkingDirectory(destination);
        launcher.setApplicationName("org.eclipse.tycho.p2.tools.publisher.TychoFeaturesAndBundlesPublisher");
        launcher.addArguments("-artifactRepository", destination.toURI().toString(), //
                "-metadataRepository", destination.toURI().toString(), //
                "-bundles", //
                bundles.stream().map(File::getAbsolutePath).collect(Collectors.joining(",")), //
                "-advices", //
                advices.stream().map(File::getAbsolutePath).collect(Collectors.joining(",")), //
                "-categoryDefinition", category.toURI().toASCIIString(), //
                "-artifactRepositoryName", //
                project.getName(), //
                "-metadataRepositoryName", //
                project.getName(), //
                "-rules", //
                "(&(classifier=osgi.bundle));mvn:${maven.groupId}:${maven.artifactId}:${maven.version}:${maven.extension}:${maven.classifier}");
        int result = launcher.execute(timeoutInSeconds);
        for (File file : advices) {
            file.delete();
        }
        category.delete();
        if (result != 0) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
    }

    private void addProperty(Properties properties, String name, String value, int i) {
        if (value != null && !value.isBlank()) {
            properties.setProperty("properties." + i + ".name", name);
            properties.setProperty("properties." + i + ".value", value);
        }

    }

    protected Set<Artifact> resolveArtifact(Artifact artifact, String scope) {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setOffline(session.isOffline());
        request.setLocalRepository(session.getLocalRepository());
        request.setResolveTransitively(scope != null && !scope.isEmpty());
        request.setRemoteRepositories(session.getCurrentProject().getRemoteArtifactRepositories());
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        return result.getArtifacts();
    }

    private File getSignatureFile(Artifact artifact) {
        final Artifact signatureArtifact = repositorySystem.createArtifactWithClassifier(artifact.getGroupId(),
                artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), artifact.getClassifier());

        signatureArtifact.setArtifactHandler(new ArtifactHandler() {

            @Override
            public boolean isIncludesDependencies() {
                return artifact.getArtifactHandler().isIncludesDependencies();
            }

            @Override
            public boolean isAddedToClasspath() {
                return artifact.getArtifactHandler().isAddedToClasspath();
            }

            @Override
            public String getPackaging() {
                return artifact.getArtifactHandler().getPackaging();
            }

            @Override
            public String getLanguage() {
                return artifact.getArtifactHandler().getLanguage();
            }

            @Override
            public String getExtension() {
                return artifact.getArtifactHandler().getExtension() + ".asc";
            }

            @Override
            public String getDirectory() {
                return artifact.getArtifactHandler().getDirectory();
            }

            @Override
            public String getClassifier() {
                return artifact.getArtifactHandler().getClassifier();
            }
        });
        for (Artifact signature : resolveArtifact(signatureArtifact, null)) {
            return signature.getFile();
        }
        return null;
    }

}
