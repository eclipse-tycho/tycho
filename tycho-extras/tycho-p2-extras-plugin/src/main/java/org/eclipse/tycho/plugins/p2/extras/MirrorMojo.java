/*******************************************************************************
 * Copyright (c) 2011, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Jan Sievers - initial API and implementation
 *    Mickael Istria (Red Hat Inc.) - 518813 Use target-platform repository
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.facade.IUDescription;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorOptions;

/**
 * Maven plugin front-end for org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication.
 * Intended as a replacement for the <a href=
 * "https://help.eclipse.org/indigo/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_repositorytasks.htm"
 * >p2.mirror ant task</a>.
 */
@Mojo(name = "mirror")
public class MirrorMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    @Component
    private EquinoxServiceFactory p2;

    @Component
    private RepositoryReferenceTool repositoryReferenceTool;

    @Component(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    /**
     * Source repositori(es) to mirror from.
     * 
     * @see also {@link #targetPlatformAsSource} and {@link #currentModuleAsSource}
     */
    @Parameter(required = false)
    private List<Repository> source;

    /**
     * The destination directory to mirror to.
     */
    @Parameter(defaultValue = "${project.build.directory}/repository")
    private File destination;

    /**
     * The target repository name.
     */
    @Parameter
    private String name;

    /**
     * (Optional) Which IUs to mirror. If omitted, all IUs available in the source repositories will
     * be mirrored. An IU must specify an id and may specify a version. If version is omitted, the
     * latest available version will be queried. By default, IUs required by the specified IUs will
     * also be mirrored. See also {@link #followStrictOnly}, {@link #followOnlyFilteredRequirements}
     * , {@link #includeOptional}, {@link #includeNonGreedy}, {@link #includeFeatures}.
     */
    @Parameter
    private List<Iu> ius;

    /**
     * Set to true if only strict dependencies should be followed. A strict dependency is defined by
     * a version range only including exactly one version (e.g. [1.0.0.v2009, 1.0.0.v2009]). In
     * particular, plugins/features included in a feature are normally required via a strict
     * dependency from the feature to the included plugin/feature.
     */
    @Parameter(defaultValue = "false")
    private boolean followStrictOnly;

    /**
     * Whether or not to include features.
     */
    @Parameter(defaultValue = "true")
    private boolean includeFeatures;

    /**
     * Whether or not to follow optional requirements.
     */
    @Parameter(defaultValue = "true")
    private boolean includeOptional;

    /**
     * Whether or not to follow non-greedy requirements.
     */
    @Parameter(defaultValue = "true")
    private boolean includeNonGreedy;

    /**
     * Filter properties. In particular, a platform filter can be specified by using keys
     * <code>osgi.os, osgi.ws, osgi.arch</code>.
     */
    @Parameter
    private Map<String, String> filter = new HashMap<>();

    /**
     * Follow only requirements which match the filter specified.
     */
    @Parameter(defaultValue = "false")
    private boolean followOnlyFilteredRequirements;

    /**
     * Set to <code>true</code> to filter the resulting set of IUs to only include the latest
     * version of each Installable Unit only. By default, all versions satisfying dependencies are
     * included.
     */
    @Parameter(defaultValue = "false")
    private boolean latestVersionOnly;

    /**
     * Whether to mirror metadata only (no artifacts).
     */
    @Parameter(defaultValue = "false")
    private boolean mirrorMetadataOnly;

    /**
     * Whether to compress the destination repository metadata files (artifacts.xml, content.xml).
     */
    @Parameter(defaultValue = "true")
    private boolean compress;

    /**
     * Whether to append to an existing destination repository. Note that appending an IU which
     * already exists in the destination repository will cause the mirror operation to fail.
     */
    @Parameter(defaultValue = "true")
    private boolean append;

    /**
     * <p>
     * Add XZ-compressed repository index files. XZ offers better compression ratios esp. for highly
     * redundant file content.
     * </p>
     * 
     * @since 0.25.0
     */
    @Parameter(defaultValue = "true")
    private boolean xzCompress;

    /**
     * <p>
     * If {@link #xzCompress} is <code>true</code>, whether jar or xml index files should be kept in
     * addition to XZ-compressed index files. This fallback provides backwards compatibility for
     * pre-Mars p2 clients which cannot read XZ-compressed index files.
     * </p>
     * 
     * @since 0.25.0
     */
    @Parameter(defaultValue = "true")
    private boolean keepNonXzIndexFiles;

    /**
     * <p>
     * Whether to add the target-platform content as a source. Ignored for non-Tycho packaging
     * types.
     * </p>
     * 
     * @since 1.1.0
     */
    @Parameter(defaultValue = "false")
    private boolean targetPlatformAsSource;

    /**
     * <p>
     * Whether the current build p2 output should be added as source. Ignored for non-Tycho
     * packaging types. Ignored if {@link #targetPlatformAsSource} == false;
     * </p>
     * 
     * @since 1.1.0
     */
    @Parameter(defaultValue = "true")
    private boolean currentModuleAsSource;

    /**
     * <p>
     * If set to true, mirroring continues to run in the event of an error during the mirroring
     * process and will just log an info message.
     * </p>
     * 
     * @since 1.1.0
     */
    @Parameter(defaultValue = "false")
    private boolean ignoreErrors;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final MirrorApplicationService mirrorService = p2.getService(MirrorApplicationService.class);

        RepositoryReferences sourceDescriptor = null;
        if (this.projectTypes.containsKey(project.getPackaging()) && this.repositoryReferenceTool != null
                && this.targetPlatformAsSource) {
            sourceDescriptor = repositoryReferenceTool.getVisibleRepositories(this.project, this.session,
                    this.currentModuleAsSource ? RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE : 0);
        } else {
            sourceDescriptor = new RepositoryReferences();
        }
        if (source != null) {
            for (final Repository sourceRepository : source) {
                if (sourceRepository.getLayout().hasMetadata()) {
                    sourceDescriptor.addMetadataRepository(sourceRepository.getLocation());
                }
                if (sourceRepository.getLayout().hasArtifacts()) {
                    sourceDescriptor.addArtifactRepository(sourceRepository.getLocation());
                }
            }
        }
        if (sourceDescriptor.getArtifactRepositories().isEmpty()
                && sourceDescriptor.getMetadataRepositories().isEmpty()) {
            throw new MojoExecutionException("No repository provided as 'source'");
        }

        if (name == null) {
            name = "";
        }
        final DestinationRepositoryDescriptor destinationDescriptor = new DestinationRepositoryDescriptor(destination,
                name, compress, xzCompress, keepNonXzIndexFiles, mirrorMetadataOnly, append, Collections.emptyMap(),
                Collections.emptyList());
        getLog().info("Mirroring to " + destination + "...");
        try {
            mirrorService.mirrorStandalone(sourceDescriptor, destinationDescriptor, createIUDescriptions(),
                    createMirrorOptions(), getBuildOutputDirectory());
        } catch (final FacadeException e) {
            throw new MojoExecutionException("Error during mirroring", e);
        }
    }

    private MirrorOptions createMirrorOptions() {
        MirrorOptions options = new MirrorOptions();
        options.setFollowOnlyFilteredRequirements(followOnlyFilteredRequirements);
        options.setFollowStrictOnly(followStrictOnly);
        options.setIncludeFeatures(includeFeatures);
        options.setIncludeNonGreedy(includeNonGreedy);
        options.setIncludeOptional(includeOptional);
        options.setLatestVersionOnly(latestVersionOnly);
        options.getFilter().putAll(filter);
        options.setIgnoreErrors(ignoreErrors);
        return options;
    }

    private Collection<IUDescription> createIUDescriptions() {
        if (ius == null) {
            return Collections.<IUDescription> emptyList();
        }
        List<IUDescription> result = new ArrayList<>();
        for (Iu iu : ius) {
            result.add(iu.toIUDescription());
        }
        return result;
    }

    private BuildDirectory getBuildOutputDirectory() {
        return DefaultReactorProject.adapt(project).getBuildDirectory();
    }
}
