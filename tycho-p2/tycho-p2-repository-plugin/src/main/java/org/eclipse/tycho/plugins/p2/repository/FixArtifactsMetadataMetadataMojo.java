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
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.util.Collections;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;

/**
 * Updates the artifact repository metadata checksums and size of modified artifacts in the given
 * folder.
 * <p>
 * This can be used if some other mojo (e.g. jar-signer) modifies the repository artifacts after the
 * assemble-repository step. An example could be found in the <a href=
 * "https://github.com/eclipse/tycho/tree/master/tycho-its/projects/jar-signing-extra">jar-signing-extra</a>
 * integration test
 * </p>
 */
@Mojo(name = "fix-artifacts-metadata", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class FixArtifactsMetadataMetadataMojo extends AbstractRepositoryMojo {
    private static final Object LOCK = new Object();
    @Parameter(defaultValue = "${project.name}")
    private String repositoryName;

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

    @Component()
    MirrorApplicationService mirrorApp;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            try {
                File destination = getAssemblyRepositoryLocation();
                if (!destination.isDirectory()) {
                    throw new MojoExecutionException(
                            "Could not update p2 repository, directory does not exist: " + destination);
                }
                DestinationRepositoryDescriptor destinationRepoDescriptor = new DestinationRepositoryDescriptor(
                        destination, repositoryName, true, xzCompress, keepNonXzIndexFiles, false, true,
                        Collections.emptyMap(), Collections.emptyList());
                mirrorApp.recreateArtifactRepository(destinationRepoDescriptor);
            } catch (FacadeException e) {
                throw new MojoExecutionException("Could not update p2 repository", e);
            }
        }
    }

}
