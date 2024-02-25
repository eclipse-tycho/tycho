/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;

/**
 * This mojo generates a target platform from all the dependencies of a maven build for example to
 * be used inside of PDE
 */
@Mojo(name = "generate-target", defaultPhase = LifecyclePhase.NONE, requiresProject = true, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, aggregator = true)
public class GenerateTargetMojo extends AbstractMojo {
    @Component
    private TychoProjectManager projectManager;

    @Component
    private MavenSession mavenSession;

    @Component
    private MavenProject mavenProject;

    @Component
    private LegacySupport legacySupport;

    @Component
    private P2RepositoryManager repositoryManager;

    @Parameter(property = "generateTargetFile", defaultValue = "${project.build.directory}/generate.target", required = true)
    private File targetFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("Scan reactor for dependencies...");
        List<MavenProject> projects = mavenSession.getProjects();
        List<IInstallableUnit> reactorDependencies = projects.stream().parallel().unordered().flatMap(project -> {
            MavenSession old = legacySupport.getSession();
            try {
                MavenSession clone = mavenSession.clone();
                clone.setCurrentProject(project);
                legacySupport.setSession(clone);
                return projectManager.getDependencyArtifacts(project).map(DependencyArtifacts::getNonReactorUnits)
                        .stream().flatMap(Collection::stream).filter(iu -> {
                            if (iu.getId().endsWith(".source")) {
                                return false;
                            }
                            if (iu.getId().endsWith(".feature.jar")) {
                                return false;
                            }
                            return true;
                        });
            } finally {
                legacySupport.setSession(old);
            }
        }).distinct().toList();
        List<String> repoList = projects.stream().flatMap(project -> {
            Stream<String> pomRepos = project.getRepositories().stream().filter(repo -> "p2".equals(repo.getLayout()))
                    .map(repo -> repo.getUrl());
            return pomRepos;
        }).distinct().sorted().toList();
        log.info("Found " + reactorDependencies.size() + " dependencies and " + repoList.size()
                + " possible repositories: ");
        Map<IMetadataRepository, Set<IInstallableUnit>> repo2unitMap = new LinkedHashMap<>();
        Set<IInstallableUnit> notFound = new HashSet<>(reactorDependencies);
        for (String repository : repoList) {
            log.info("\tScanning " + repository + "...");
            try {
                IMetadataRepository metadataRepository = repositoryManager
                        .getMetadataRepository(new MavenRepositoryLocation(null, URI.create(repository)));
                Set<IInstallableUnit> units = new HashSet<>();
                for (IInstallableUnit unit : reactorDependencies) {
                    if (metadataRepository.contains(unit)) {
                        units.add(unit);
                        notFound.remove(unit);
                    }
                }
                if (units.size() > 0) {
                    repo2unitMap.put(metadataRepository, units);
                    log.info("\tFound: " + units.size() + " dependencies in this repository!");
                }
            } catch (ProvisionException e) {
                throw new MojoFailureException("can't load repository " + repository, e);
            }
        }
        if (notFound.size() > 0) {
            log.info(notFound.size() + " dependencies where not mapped to a repository:");
            for (IInstallableUnit unit : notFound) {
                log.info("\t" + unit);
            }
        }
        log.info("Generate Target ...");
        StringBuilder builder = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<?pde version=\"3.8\"?>\n<target name=\"" + mavenProject.getName() + "\">\n\t<locations>\n");
        for (Entry<IMetadataRepository, Set<IInstallableUnit>> entry : repo2unitMap.entrySet()) {
            builder.append("\t\t<location includeMode=\"planner\" type=\"InstallableUnit\">\n");
            Set<IInstallableUnit> requiredUnits = entry.getValue();
            //First step is to remove everything that is already required by something...
            Set<IInstallableUnit> alreayTransitiveRequired = computeRequired(requiredUnits);
            requiredUnits.removeAll(alreayTransitiveRequired);
            IMetadataRepository repository = entry.getKey();
            if (!hasOnlyFeatures(requiredUnits)) {
                Set<IInstallableUnit> strict = new HashSet<>(requiredUnits);
                //Next is we try to find all features that do require something we still want to have
                IQueryResult<IInstallableUnit> features = repository.query(QueryUtil.createIUGroupQuery(), null);
                Set<IInstallableUnit> providingFeatures = computeFeatureRequires(features, requiredUnits);
                requiredUnits.addAll(providingFeatures);
                //Now a final reduction step
                Set<IInstallableUnit> transitiveRequiredFinal = computeRequired(requiredUnits);
                requiredUnits.removeAll(transitiveRequiredFinal);
                if (!strict.equals(requiredUnits)) {
                    builder.append("\t\t\t<!-- List of strictly required units -->\n");
                    for (IInstallableUnit unit : strict) {
                        builder.append("\t\t\t<!-- ");
                        addUnit(unit, builder);
                        builder.append("  -->\n");
                    }
                    builder.append(
                            "\n\t\t\t<!-- Required units using features, this might include more than is strictly required -->\n");
                }
            }
            for (IInstallableUnit unit : requiredUnits) {
                builder.append("\t\t\t");
                addUnit(unit, builder);
                builder.append("\n");
            }
            builder.append("\t\t\t<repository location=\"" + repository.getLocation() + "\"/>\n");
            builder.append("\t\t</location>\n");
        }
        if (notFound.size() > 0) {
            List<String> osgiMaven = new ArrayList<>();
            List<String> wrappedMaven = new ArrayList<>();
            for (IInstallableUnit mavenUnit : notFound) {
                String osgi = getMavenDependency(mavenUnit, builder, TychoConstants.PROP_GROUP_ID,
                        TychoConstants.PROP_ARTIFACT_ID, TychoConstants.PROP_VERSION);
                if (osgi == null) {
                    String wrapped = getMavenDependency(mavenUnit, builder, TychoConstants.PROP_WRAPPED_GROUP_ID,
                            TychoConstants.PROP_WRAPPED_ARTIFACT_ID, TychoConstants.PROP_WRAPPED_VERSION);
                    if (wrapped != null) {
                        wrappedMaven.add(wrapped);
                    } else {
                        log.warn(mavenUnit + " can not be represented in the target at all!");
                    }
                } else {
                    osgiMaven.add(osgi);
                }
            }
            createMavenLocation(osgiMaven, "error", builder);
            createMavenLocation(wrappedMaven, "generate", builder);
        }
        builder.append("\t</locations>\n</target>");
        try {
            targetFile.getParentFile().mkdirs();
            Files.writeString(targetFile.toPath(), builder, StandardCharsets.UTF_8);
            log.info("Target is written to " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoFailureException("writing target file failed", e);
        }
    }

    private void createMavenLocation(List<String> artifacts, String mm, StringBuilder builder) {
        builder.append("\t\t<location includeDependencyDepth=\"none\" missingManifest=\"");
        builder.append(mm);
        builder.append("\" type=\"Maven\">\n");
        for (String artifact : artifacts) {
            builder.append("\t\t\t");
            builder.append(artifact);
            builder.append("\n");
        }
        builder.append("\t\t</location>\n");
    }

    private String getMavenDependency(IInstallableUnit mavenUnit, StringBuilder builder, String groupProperty,
            String artifactProperty, String versionProperty) {
        String groupId = mavenUnit.getProperty(groupProperty);
        String artifactId = mavenUnit.getProperty(artifactProperty);
        String version = mavenUnit.getProperty(versionProperty);
        if (groupId != null && artifactId != null && version != null) {
            return String.format(
                    "<dependency>\n\t\t\t\t<groupId>%s</groupId>\n\t\t\t\t<artifactId>%s</artifactId>\n\t\t\t\t<version>%s</version>\n\t\t\t</dependency>",
                    groupId, artifactId, version);
        }
        return null;
    }

    private boolean hasOnlyFeatures(Set<IInstallableUnit> requiredUnits) {
        for (IInstallableUnit unit : requiredUnits) {
            if (!Boolean.TRUE.toString().equals(unit.getProperty(QueryUtil.PROP_TYPE_GROUP))) {
                return false;
            }
        }
        return true;
    }

    private void addUnit(IInstallableUnit unit, StringBuilder builder) {
        builder.append("<unit id=\"");
        builder.append(unit.getId());
        builder.append("\" version=\"");
        builder.append(unit.getVersion());
        builder.append("\"/>");
    }

    private Set<IInstallableUnit> computeFeatureRequires(IQueryResult<IInstallableUnit> features,
            Set<IInstallableUnit> units) {
        HashSet<IInstallableUnit> result = new HashSet<>();
        outer: for (IInstallableUnit feature : features) {
            if (units.contains(feature)) {
                continue;
            }
            if (Boolean.TRUE.toString()
                    .equals(feature.getProperty(MetadataFactory.InstallableUnitDescription.PROP_TYPE_PRODUCT))) {
                continue;
            }
            for (IRequirement featureRequirement : feature.getRequirements()) {
                for (IInstallableUnit unit : units) {
                    if (unit.satisfies(featureRequirement)) {
                        result.add(feature);
                        continue outer;
                    }
                }
            }
        }
        return result;
    }

    private Set<IInstallableUnit> computeRequired(Set<IInstallableUnit> units) {
        Set<IInstallableUnit> required = new HashSet<>();
        for (IInstallableUnit base : units) {
            for (IRequirement requirement : base.getRequirements()) {
                for (IInstallableUnit other : units) {
                    if (other == base) {
                        continue;
                    }
                    if (other.satisfies(requirement)) {
                        required.add(other);
                    }
                }
            }
        }
        return required;
    }

}
