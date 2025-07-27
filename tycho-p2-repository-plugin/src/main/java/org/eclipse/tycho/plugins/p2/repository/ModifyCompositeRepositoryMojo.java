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
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.helper.StatusTool;
import org.eclipse.tycho.p2maven.repository.P2RepositoryKind;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.eclipse.tycho.p2maven.tools.P2RepositoryDataManipulator;
import org.eclipse.tycho.p2maven.tools.P2RepositoryDataManipulator.ModifiedRepositoryDescriptor;
import org.eclipse.tycho.p2maven.tools.P2RepositoryDataManipulator.RepositoryLocationURISyntaxException;

/**
 * Modifies the content or name of a composite P2-repository.
 * <p>
 * This can be used to create a new composite repository or to modify an already existing (remote)
 * repository. The resulting repository metadata files ({@code compositeArtifacts.xml/jar} or
 * {@code compositeContent.xml/jar}) are always written to a local directory. For a remote
 * repository, a suitable other tool (such as SSH) must be used to copy these files subsequently
 * back to the remote server.
 * </p>
 */
@Mojo(name = "modify-composite-repository")
public class ModifyCompositeRepositoryMojo extends AbstractMojo {

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
    /** The children to append to the composite repository. */
    @Parameter(property = "p2.composite.children.add")
    private List<URI> childrenToAdd = List.of();
    /** The children to remove from the composite repository. */
    @Parameter(property = "p2.composite.children.remove")
    private List<URI> childrenToRemove = List.of();

    /**
     * The maximum number of children to keep in the composite repository.<br>
     * If this limit is exceeded (after adding children) a corresponding count is removed from the
     * beginning of the list of children. A value of zero or less, is considered as no-limit.
     */
    @Parameter(property = "p2.composite.children.limit", defaultValue = "0")
    private int childCountLimit = 0;

    /** Validate the existence of the repository and all added children. */
    @Parameter(property = "p2.composite.validate", defaultValue = "true")
    private boolean validateChildren;

    /** Compress the repository index files {@code content.xml} and {@code artifacts.xml}. */
    @Parameter(property = "p2.repository.compress", defaultValue = "true")
    private boolean compress;

    @Inject
    private P2RepositoryDataManipulator manipulator;
    @Inject
    private Logger log;
    @Inject
    private P2RepositoryManager manager;
    @Inject
    private IProvisioningAgent agent;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Get transport service here to trigger its information print-out early, avoiding it in-between other information that are then disrupted.
        agent.getService(org.eclipse.equinox.internal.p2.repository.Transport.class);
        try {
            ModifiedRepositoryDescriptor descriptor = P2RepositoryDataManipulator.createDescriptor(repository,
                    repositoryKind, outputLocation, compress, false, false);

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
            printChildren(childrenToRemove, "Remove");
            printChildren(childrenToAdd, "Add");
            if (childCountLimit > 0) {
                log.info("Limiting number of children to " + childCountLimit);
            }
            if (validateChildren && !childrenToAdd.isEmpty()) {
                for (URI child : childrenToAdd) {
                    validateChildRepository(child, descriptor);
                }
            }

            manipulator.modifyCompositeRepository(descriptor, repositoryName, childrenToAdd, childrenToRemove,
                    childCountLimit);
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

    private void validateChildRepository(URI child, ModifiedRepositoryDescriptor baseRepository)
            throws ProvisionException {
        MavenRepositoryLocation repositoryLocation = baseRepository.repository();
        URI childLocation = repositoryLocation.getURL().resolve(child);
        log.info("Verifying p2 repository child at " + childLocation);
        String artifacts = "";
        String units = "";
        if (baseRepository.isArtifact()) {
            artifacts = manager.allArtifacts(childLocation, repositoryLocation.getId()).stream().count() + " artifacts";
        }
        if (baseRepository.isMetadata()) {
            units = manager.allMetadataUnits(childLocation, repositoryLocation.getId()).stream().count() + " units";
        }
        log.info("  Found " + artifacts + (!artifacts.isEmpty() && !units.isEmpty() ? " and " : "") + units + ".");
    }

    private void printChildren(List<URI> children, String action) {
        if (!children.isEmpty()) {
            log.info(action + " children:");
            for (URI child : children) {
                log.info("  " + child);
            }
        }
    }

}
