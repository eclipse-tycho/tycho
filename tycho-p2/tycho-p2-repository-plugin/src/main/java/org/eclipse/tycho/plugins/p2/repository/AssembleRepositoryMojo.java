/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.EclipseRepositoryProject;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReference;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;

/**
 * <p>
 * Aggregates content into a p2 repository in <code>${project.build.directory}/repository</code>.
 * </p>
 * <p>
 * <ol>
 * <li>Copies resources (if any) from <code>${project.build.outputDirectory}</code> to
 * <code>${project.build.directory}/repository</code>. This allows to include additional files such
 * as <code>index.html</code> or about files from <code>src/main/resources</code> (or elsewhere)
 * into the p2 repository.</li>
 * <li>The p2 aggregation into <code>${project.build.directory}/repository</code> runs recursively:
 * it starts with the content published in the current module, and traverses all artifacts that are
 * marked as <em>included</em> in already aggregated artifacts. (The following artifacts can include
 * other artifacts: categories, products, and features. Note: Dependencies with a strict version
 * range, i.e. a range which only matches exactly one version of an artifact, are also considered as
 * inclusions.)</li>
 * </ol>
 * </p>
 * 
 */
@Mojo(name = "assemble-repository", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class AssembleRepositoryMojo extends AbstractRepositoryMojo {
    private static final Object LOCK = new Object();
    /**
     * <p>
     * By default, this goal creates a p2 repository. Set this to <code>false</code> if only a p2
     * metadata repository (without the artifact files) shall be created.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean createArtifactRepository;

    /**
     * <p>
     * By default, only (transitive) <em>inclusions</em> of the published artifacts are aggregated.
     * Set this parameter to <code>true</code> to aggregate <em>all transitive dependencies</em>,
     * making the resulting p2 repository self-contained.
     * </p>
     */
    @Parameter(defaultValue = "false")
    private boolean includeAllDependencies;

    /**
     * <p>
     * Compress the repository index files <tt>content.xml</tt> and <tt>artifacts.xml</tt>.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean compress;

    /**
     * <p>
     * Add XZ-compressed repository index files. XZ offers better compression ratios esp. for highly
     * redundant file content.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean xzCompress;

    /**
     * <p>
     * If {@link #xzCompress} is <code>true</code>, whether jar or xml index files should be kept in
     * addition to XZ-compressed index files. This fallback provides backwards compatibility for
     * pre-Mars p2 clients which cannot read XZ-compressed index files.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean keepNonXzIndexFiles;

    /**
     * <p>
     * The name attribute stored in the created p2 repository.
     * </p>
     */
    @Parameter(defaultValue = "${project.name}")
    private String repositoryName;

    /**
     * <p>
     * Additional properties against which p2 filters are evaluated while aggregating.
     * </p>
     */
    @Parameter
    private Map<String, String> profileProperties;

    @Parameter
    private Map<String, String> extraArtifactRepositoryProperties;

    @Component
    private RepositoryReferenceTool repositoryReferenceTool;

    @Component
    private EquinoxServiceFactory p2;

    @Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_REPOSITORY)
    private EclipseRepositoryProject eclipseRepositoryProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            try {
                File destination = getAssemblyRepositoryLocation();
                destination.mkdirs();
                copyResources(destination);

                Collection<DependencySeed> projectSeeds = TychoProjectUtils.getDependencySeeds(getReactorProject());
                if (projectSeeds.isEmpty()) {
                    throw new MojoFailureException("No content specified for p2 repository");
                }

                RepositoryReferences sources = getVisibleRepositories();

                MirrorApplicationService mirrorApp = p2.getService(MirrorApplicationService.class);

                List<RepositoryReference> repositoryRefrences = getCategories().stream()//
                        .map(Category::getRepositoryReferences)//
                        .flatMap(List::stream)//
                        .map(ref -> new RepositoryReference(ref.getName(), ref.getLocation(), ref.isEnabled()))//
                        .collect(toList());

                DestinationRepositoryDescriptor destinationRepoDescriptor = new DestinationRepositoryDescriptor(
                        destination, repositoryName, compress, xzCompress, keepNonXzIndexFiles,
                        !createArtifactRepository, true, extraArtifactRepositoryProperties, repositoryRefrences);
                mirrorApp.mirrorReactor(sources, destinationRepoDescriptor, projectSeeds, getBuildContext(),
                        includeAllDependencies, profileProperties);
            } catch (FacadeException e) {
                throw new MojoExecutionException("Could not assemble p2 repository", e);
            }
        }
    }

    private void copyResources(File destination) throws MojoExecutionException {
        File outputDir = new File(getProject().getBuild().getOutputDirectory());
        try {
            if (outputDir.isDirectory()) {
                getLog().info(String.format("Copying resources from %s to %s", outputDir, destination));
                FileUtils.copyDirectoryStructure(outputDir, destination);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying resources", e);
        }
    }

    protected RepositoryReferences getVisibleRepositories() throws MojoExecutionException, MojoFailureException {
        int flags = RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE;
        return repositoryReferenceTool.getVisibleRepositories(getProject(), getSession(), flags);
    }

    private List<Category> getCategories() {
        return eclipseRepositoryProject.loadCategories(getReactorProject());
    }

}
