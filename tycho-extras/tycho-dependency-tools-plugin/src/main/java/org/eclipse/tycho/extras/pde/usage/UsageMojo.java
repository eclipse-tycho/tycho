/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde.usage;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ExecutionEnvironment;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.ee.impl.StandardEEResolutionHints;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolverService;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;

/**
 * This mojos compares the actual content of the target against what is used in the projects to
 * allow for getting rid of seldom or unused dependencies
 * 
 * Example <code>org.eclipse.tycho.extras:tycho-dependency-tools-plugin:6.0.0-SNAPSHOT:usage</code>
 */
@Mojo(name = "usage", defaultPhase = LifecyclePhase.NONE, requiresProject = true, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, aggregator = true)
public class UsageMojo extends AbstractMojo {

    @Parameter(defaultValue = "tree", property = "usage.layout")
    private String layout;

    @Parameter(property = "verbose")
    private boolean verbose;

    @Component
    private TychoProjectManager projectManager;

    @Component
    private MavenSession mavenSession;

    @Component
    private LegacySupport legacySupport;

    @Component
    private P2RepositoryManager repositoryManager;

    @Component
    private TargetDefinitionResolverService definitionResolverService;

    @Component
    private IProvisioningAgent agent;

    @Component
    private Map<String, ReportLayout> layouts;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ReportLayout reportLayout = Objects.requireNonNull(layouts.get(layout),
                "The layout " + layout + " can not be found");
        Log log = getLog();
        log.info("Scan reactor for dependencies...");
        List<MavenProject> projects = mavenSession.getProjects();
        UsageReport usageReport = new UsageReport();
        for (MavenProject project : projects) {
            Set<IInstallableUnit> projectUnits = projectManager.getDependencyArtifacts(project)
                    .map(DependencyArtifacts::getNonReactorUnits).stream().flatMap(Collection::stream).filter(iu -> {
                        if (iu.getId().endsWith(".source")) {
                            return false;
                        }
                        if (iu.getId().endsWith(".feature.jar")) {
                            return false;
                        }
                        return true;
                    }).collect(Collectors.toSet());
            usageReport.projectUsage.put(project, projectUnits);
            usageReport.usedUnits.addAll(projectUnits);
            TargetPlatformConfiguration tpconfig = projectManager.getTargetPlatformConfiguration(project);
            List<TargetDefinitionFile> targets = tpconfig.getTargets();
            ExecutionEnvironment specification = projectManager.getExecutionEnvironmentConfiguration(project)
                    .getFullSpecification();
            TargetDefinitionResolver targetDefinitionResolver = new TargetDefinitionResolver() {
                @Override
                public TargetDefinition getTargetDefinition(URI uri) {
                    return TargetDefinitionFile.read(uri);
                }

                @Override
                public TargetDefinitionContent fetchContent(TargetDefinition definition) {
                    return definitionResolverService.getTargetDefinitionContent(definition,
                            List.of(TargetEnvironment.getRunningEnvironment()),
                            new StandardEEResolutionHints(specification), IncludeSourceMode.ignore,
                            ReferencedRepositoryMode.include, agent);
                }
            };
            for (TargetDefinitionFile definitionFile : targets) {
                usageReport.analyzeLocations(definitionFile, targetDefinitionResolver,
                        (l, e) -> log.warn("Can't analyze location " + l, e));
            }
        }
        reportLayout.generateReport(usageReport, verbose, log::info);
    }

}
