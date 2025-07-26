/*******************************************************************************
 * Copyright (c) 2025, 2025 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.helper.StatusTool;
import org.eclipse.tycho.p2maven.repository.P2RepositoryKind;
import org.eclipse.tycho.p2maven.tools.P2RepositoryDataManipulator;
import org.eclipse.tycho.p2maven.tools.P2RepositoryDataManipulator.ModifiedRepositoryDescriptor;
import org.eclipse.tycho.p2maven.tools.P2RepositoryDataManipulator.RepositoryLocationURISyntaxException;

/**
 * Modifies metadata of a P2-repository like its name or properties.
 * <p>
 * This can be used to customize metadata of a just assembled repository or to modify an already
 * existing (remote) repository. The modified repository metadata files
 * ({@code artifact.xml/jar/xml.xz} or {@code content.xml/jar/xml.xz}) are always written to a local
 * file. For a remote repository a suitable other tool (like SSH) must be used to copy these files
 * subsequently back to the remote server.
 * </p>
 */
@Mojo(name = "modify-repository-properties", threadSafe = true)
public class ModifyRepositoryPropertiesMojo extends AbstractMojo {

    /**
     * Location of the <em>source</em> repository to modify.<br>
     * May be the {@code Path} of a local file or {@code URL}/{@code URI} to a remote
     * repository.<br>
     * If this is set to a remote URL the {@link #outputLocation} must be set to a local path as
     * remote locations cannot be written.
     */
    @Parameter
    private Repository repository;

    /**
     * Optional output location where the modified metadata files ({@code artifact.xml/jar/xml.xz}
     * or {@code content.xml/jar/xml.xz}) are written to.<br>
     * If not specified or equals to {@link #repository} the source repository is modified in-place.
     */
    @Parameter(property = "p2.repository.output")
    private Path outputLocation;

    /**
     * The kind of modified repository: Either {@code artifact}, {@code metadata}.<br>
     * If {@code artifact} is specified, only the {@code artifact.xml/jar/xml.xz} files are
     * modified, if {@code metadata} only {@code content.xml/jar/xml.xz}. By default (if blank) both
     * are modified.
     */
    @Parameter(property = "p2.repository.kind")
    private P2RepositoryKind repositoryKind;

    /** Optional repository name to set. An existing name is overwritten. */
    @Parameter(property = "p2.repository.name")
    private String repositoryName;
    /**
     * The properties to remove.<br>
     * Properties that don't exist in the repository are just silently ignored.
     */
    @Parameter(property = "p2.repository.properties.remove")
    private List<String> propertiesToRemove = List.of();
    /**
     * The properties to add.<br>
     * Already existing properties are overwritten and updated to the value specified.
     */
    @Parameter(property = "p2.repository.properties.add")
    private Map<String, String> propertiesToAdd = Map.of();

    /** Compress the repository index files {@code content.xml} and {@code artifacts.xml}. */
    @Parameter(property = "p2.repository.compress", defaultValue = "true")
    private boolean compress;

    /**
     * Add XZ-compressed repository index files. XZ offers better compression ratios esp. for highly
     * redundant file content.
     */
    @Parameter(property = "p2.repository.xz", defaultValue = "true")
    private boolean xzCompress;

    /**
     * If {@link #xzCompress} is {@code true}, whether jar or xml index files should be kept in
     * addition to XZ-compressed index files. This fallback provides backwards compatibility for
     * pre-Mars p2 clients which cannot read XZ-compressed index files.
     */
    @Parameter(property = "p2.repository.xz.keep", defaultValue = "true")
    private boolean keepNonXzIndexFiles;

    @Inject
    private P2RepositoryDataManipulator manipulator;
    @Inject
    private Logger log;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ModifiedRepositoryDescriptor descriptor = P2RepositoryDataManipulator.createDescriptor(repository,
                    repositoryKind, outputLocation, compress, xzCompress, keepNonXzIndexFiles);

            String type = "repository";
            if (repositoryKind != null) {
                type = (repositoryKind == P2RepositoryKind.artifact ? "artifact" : "metadata") + " repository";
            }
            Path output = descriptor.outputLocation();
            if (output != null) {
                log.info("Write modified " + type + " file(s) to " + output);
            } else {
                log.info("Modify " + type + " in-place at " + descriptor.repository().getURL());
            }
            if (repositoryName != null) {
                log.info("Set name to: " + repositoryName);
            }
            if (!propertiesToRemove.isEmpty()) {
                log.info("Remove properties: " + String.join(", ", propertiesToRemove));
            }
            if (!propertiesToAdd.isEmpty()) {
                log.info("Add properties:");
                propertiesToAdd.forEach((key, value) -> {
                    log.info("  " + key + " = " + value);
                });
            }

            manipulator.modifyRepositoryMetadata(descriptor, repositoryName, propertiesToRemove, propertiesToAdd);
        } catch (RepositoryLocationURISyntaxException e) {
            throw new MojoFailureException("Illegal repository location URL: " + repository.getUrl(), e.getCause());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to modify repository " + repository.getUrl(), e);
        } catch (ProvisionException e) {
            log.error(StatusTool.toLogMessage(e.getStatus()), e);
            throw new MojoExecutionException("Failed to modify repository " + repository.getUrl() + ": "
                    + StatusTool.collectProblems(e.getStatus()), e);
        }
    }

}
