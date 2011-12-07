/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jan Sievers - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.facade.IUDescription;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorOptions;

/**
 * Maven plugin front-end for org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication.
 * Intended as a replacement for the <a href=
 * "http://help.eclipse.org/indigo/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_repositorytasks.htm"
 * >p2.mirror ant task</a>.
 * 
 * @goal mirror
 */
public class MirrorMojo extends AbstractMojo {

    /** @parameter expression="${project}" */
    private MavenProject project;

    /** @component */
    private EquinoxServiceFactory p2;

    /**
     * Source repositori(es) to mirror from.
     * 
     * @parameter
     * @required
     */
    private List<Repository> source;

    /**
     * The destination directory to mirror to.
     * 
     * @parameter default-value="${project.build.directory}/repository"
     */
    private File destination;

    /**
     * (Optional) Which IUs to mirror. If omitted, all IUs available in the source repositories will
     * be mirrored. An IU must specify an id and may specify a version. If version is omitted, the
     * latest available version will be queried. By default, IUs required by the specified IUs will
     * also be mirrored. See also {@link #followStrictOnly}, {@link #followOnlyFilteredRequirements}
     * , {@link #includeOptional}, {@link #includeNonGreedy}, {@link #includeFeatures}.
     * 
     * @parameter
     */
    private List<Iu> ius;

    /**
     * Set to true if only strict dependencies should be followed. A strict dependency is defined by
     * a version range only including exactly one version (e.g. [1.0.0.v2009, 1.0.0.v2009]). In
     * particular, plugins/features included in a feature are normally required via a strict
     * dependency from the feature to the included plugin/feature.
     * 
     * @parameter default-value="false"
     */
    private boolean followStrictOnly;
    /**
     * Whether or not to include features.
     * 
     * @parameter default-value="true"
     */
    private boolean includeFeatures;
    /**
     * Whether or not to follow optional requirements.
     * 
     * @parameter default-value="true"
     */
    private boolean includeOptional;
    /**
     * Whether or not to follow non-greedy requirements.
     * 
     * @parameter default-value="true"
     */
    private boolean includeNonGreedy;
    /**
     * Filter properties. In particular, a platform filter can be specified by using keys
     * <code>osgi.os, osgi.ws, osgi.arch</code>.
     * 
     * @parameter
     */
    private Map<String, String> filter = new HashMap<String, String>();
    /**
     * Follow only requirements which match the filter specified.
     * 
     * @parameter default-value="false"
     */
    private boolean followOnlyFilteredRequirements;

    /**
     * Set to <code>true</code> to filter the resulting set of IUs to only include the latest
     * version of each Installable Unit only. By default, all versions satisfying dependencies are
     * included.
     * 
     * @parameter default-value="false"
     */
    private boolean latestVersionOnly;

    /**
     * Whether to mirror metadata only (no artifacts).
     * 
     * @parameter default-value="false"
     */
    private boolean mirrorMetadataOnly;

    /**
     * Whether to compress the destination repository metadata files (artifacts.xml, content.xml).
     * 
     * @parameter default-value="true"
     */
    private boolean compress;

    /**
     * Whether to append to an existing destination repository. Note that appending an IU which
     * already exists in the destination repository will cause the mirror operation to fail.
     * 
     * @parameter default-value="true"
     */
    private boolean append;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final MirrorApplicationService mirrorService = p2.getService(MirrorApplicationService.class);

        final RepositoryReferences sourceDescriptor = new RepositoryReferences();
        for (final Repository sourceRepository : source) {
            if (sourceRepository.getLayout().hasMetadata()) {
                sourceDescriptor.addMetadataRepository(sourceRepository.getLocation());
            }
            if (sourceRepository.getLayout().hasArtifacts()) {
                sourceDescriptor.addArtifactRepository(sourceRepository.getLocation());
            }
        }

        final DestinationRepositoryDescriptor destinationDescriptor = new DestinationRepositoryDescriptor(destination,
                "", compress, mirrorMetadataOnly, append);
        getLog().info("Mirroring to " + destination + "...");
        try {
            mirrorService.mirrorStandalone(sourceDescriptor, destinationDescriptor, createIUDescriptions(),
                    createMirrorOptions(), new File(project.getBuild().getDirectory()));
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
        return options;
    }

    private Collection<IUDescription> createIUDescriptions() {
        if (ius == null) {
            return Collections.<IUDescription> emptyList();
        }
        List<IUDescription> result = new ArrayList<IUDescription>();
        for (Iu iu : ius) {
            result.add(iu.toIUDescription());
        }
        return result;
    }
}
