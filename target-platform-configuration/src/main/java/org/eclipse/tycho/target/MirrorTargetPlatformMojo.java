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
package org.eclipse.tycho.target;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.updatesite.SiteCategory;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.repository.ListCompositeMetadataRepository;
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2maven.InstallableUnitSlicer;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;
import org.eclipse.tycho.p2maven.SlicingOptions;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;

/**
 * Supports mirroring the computed target platform of the current project, this behaves similar to
 * what PDE offers with its export deployable feature / plug-in and assembles an update site that
 * contains everything this particular project depends on.
 */
@Mojo(name = "mirror-target-platform", threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class MirrorTargetPlatformMojo extends AbstractMojo {

    private static final SiteXMLAction CATEGORY_FACTORY = new SiteXMLAction((URI) null, (String) null);

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/target-platform-repository")
    private File destination;

    @Parameter(defaultValue = "${project.id}")
    private String name;

    @Parameter(defaultValue = "true")
    private boolean includeCategories = true;

    /**
     * Allows configuration of additional slicing options for example like this:
     * 
     * <pre>
     * &lt;options&gt;
    *   &lt;!-- should optional dependencies be included (true) or ignored (false), defaults to true --&gt;
    *   &lt;includeOptionalDependencies&gt;true/false&lt;/includeOptionalDependencies&gt;
    *   &lt;!-- should requirements be considered always greedy (true) - i.e., install even if there are no usages - or only if specified (false), defaults to true --&gt;
    *   &lt;everythingGreedy&gt;true/false&lt;/everythingGreedy&gt;
    *   &lt;!-- should only strict dependencies be considered (true) or all dependencies (false), defaults to false --&gt;
    *   &lt;!-- a strict dependency is one with a strict version range (e.g. [1.2,1.2]) and usually maps to items included in a feature, default is false --&gt;
    *   &lt;considerStrictDependencyOnly&gt;true/false&lt;/considerStrictDependencyOnly&gt;
    *   &lt;!-- should only items that have a filter be considered (true) or all of them (false), default is false --&gt;
    *   &lt;followOnlyFilteredRequirements&gt;true/false&lt;/followOnlyFilteredRequirements&gt;
    *   &lt;!-- if no filter context is defined and a filter is encountered (e.g., os = win32 on a requirement), the filter will always match (true) or fail (false), default is true --&gt;
    *   &lt;forceFilterTo&gt;true/false&lt;/forceFilterTo&gt;
    *   &lt;!-- should only the latest version (true) or all versions (false) be included, default is true --&gt;
    *   &lt;latestVersion&gt;true/false&lt;/latestVersion&gt;
    *   &lt;!-- defines the filter context, if not given, filtering is disabled and the value of forceFilterTo is used --&gt;
    *   &lt;filter&gt;
    *       &lt;key&gt;value&lt;/key&gt;
    *            ...
    *   &lt;/filter&gt;
    *  &lt;/options&gt;
     * </pre>
     */
    @Parameter
    private SlicingOptions options;

    @Component
    private TargetPlatformService platformService;

    @Component
    private MirrorApplicationService mirrorService;

    @Component
    private ReactorRepositoryManager repositoryManager;

    @Component
    private IProvisioningAgent agent;

    @Component
    private InstallableUnitSlicer installableUnitSlicer;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        TargetPlatform targetPlatform = platformService.getTargetPlatform(reactorProject).orElse(null);
        if (targetPlatform == null) {
            getLog().info("Project has no target platform, skip execution.");
            return;
        }
        IArtifactRepository sourceArtifactRepository = targetPlatform.getArtifactRepository();
        IMetadataRepository sourceMetadataRepository = targetPlatform.getMetadataRepository();
        PublishingRepository publishingRepository = repositoryManager.getPublishingRepository(reactorProject);
        try {
            IMetadataRepository projectRepository = publishingRepository.getMetadataRepository();
            IArtifactRepository artifactRepository = new ListCompositeArtifactRepository(
                    List.of(sourceArtifactRepository, publishingRepository.getArtifactRepository()), agent);
            IMetadataRepository metadataRepository = new ListCompositeMetadataRepository(
                    List.of(sourceMetadataRepository, projectRepository), agent);
            IQueryable<IInstallableUnit> mirrorUnits;
            if (PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION.equals(project.getPackaging())) {
                //for a target platform we like to mirror everything...
                mirrorUnits = metadataRepository;
            } else {
                //for everything else we want to mirror only items that are required by the project
                try {
                    IQueryResult<IInstallableUnit> query = projectRepository.query(QueryUtil.ALL_UNITS, null);
                    Set<IInstallableUnit> rootIus = query.toSet();
                    String label;
                    String projectName = project.getName();
                    if (projectName != null && !projectName.isBlank()) {
                        label = projectName;
                    } else {
                        label = project.getId();
                    }
                    rootIus.add(createCategory(label, query));
                    mirrorUnits = installableUnitSlicer.computeDependencies(rootIus, metadataRepository, options);
                } catch (CoreException e) {
                    throw new MojoFailureException("Failed to compute dependencies to mirror", e);
                }
            }
            Set<IInstallableUnit> toMirror = mirrorUnits.query(QueryUtil.ALL_UNITS, null).toSet();
            if (!includeCategories) {
                //remove any categories from the result
                toMirror.removeIf(QueryUtil::isCategory);
            }
            getLog().info(
                    "Mirroring " + toMirror.size() + " unit(s) from the target platform, this can take a while ...");
            mirrorService.mirrorDirect(artifactRepository, new CollectionResult<IInstallableUnit>(toMirror),
                    destination, name);
        } catch (FacadeException e) {
            throw new MojoFailureException(e.getMessage(), e.getCause());
        }
    }

    private static IInstallableUnit createCategory(String label, IQueryResult<IInstallableUnit> result) {
        SiteCategory category = new SiteCategory();
        category.setLabel(label);
        category.setName("generated.project.category." + UUID.randomUUID());
        return CATEGORY_FACTORY.createCategoryIU(category,
                result.stream().filter(iu -> !iu.getId().endsWith(".feature.jar")).collect(Collectors.toSet()));
    }

}
