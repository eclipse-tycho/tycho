/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.mirroring;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.mirroring.IArtifactMirrorLog;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.TargetEnvironment;
import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2.util.StatusTool;

@SuppressWarnings("restriction")
public class MirrorApplicationServiceImpl implements MirrorApplicationService {

    private static final String MIRROR_FAILURE_MESSAGE = "Copying p2 repository content failed";

    public void mirror(RepositoryReferences sources, DestinationRepositoryDescriptor destination,
            Collection<?> rootUnits, BuildContext context, int flags, MavenLogger logger) throws FacadeException {
        IProvisioningAgent agent = Activator.createProvisioningAgent(context.getTargetDirectory());
        try {
            final MirrorApplication mirrorApp = new MirrorApplication(agent);

            setSourceRepositories(mirrorApp, sources);

            final RepositoryDescriptor destinationDescriptor = new RepositoryDescriptor();
            destinationDescriptor.setLocation(destination.getLocation().toURI());
            destinationDescriptor.setAppend(true);
            destinationDescriptor.setName(destination.getName());
            boolean compressed = (flags & REPOSITORY_COMPRESS) != 0;
            destinationDescriptor.setCompressed(compressed);
            if ((flags & MIRROR_ARTIFACTS) != 0) {
                // metadata and artifacts is the default
            } else {
                // only mirror metadata
                destinationDescriptor.setKind(RepositoryDescriptor.KIND_METADATA);
            }
            mirrorApp.addDestination(destinationDescriptor);

            if (rootUnits == null) {
                // mirror everything
            } else {
                mirrorApp.setSourceIUs(toInstallableUnitList(rootUnits));
            }

            final SlicingOptions options = new SlicingOptions();
            boolean includeAllDepenendcies = (flags & INCLUDE_ALL_DEPENDENCIES) != 0;
            options.considerStrictDependencyOnly(!includeAllDepenendcies);

            List<TargetEnvironment> environments = context.getEnvironments();
            if (environments == null) {
                mirrorForAllEnvironments(mirrorApp, options, logger);
            } else {
                mirrorForSpecifiedEnvironments(mirrorApp, options, environments, logger);
            }
        } finally {
            agent.stop();
        }
    }

    private void mirrorForAllEnvironments(final MirrorApplication mirrorApp, final SlicingOptions options,
            MavenLogger logger) throws FacadeException {
        options.forceFilterTo(true);
        executeMirroring(mirrorApp, options, logger);
    }

    private void mirrorForSpecifiedEnvironments(final MirrorApplication mirrorApp, final SlicingOptions options,
            List<TargetEnvironment> environments, MavenLogger logger) throws FacadeException {
        // TODO the p2 mirror tool should support mirroring multiple environments at once
        for (TargetEnvironment environment : environments) {
            Map<String, String> filter = environment.toFilter();
            addFilterForFeatureJARs(filter);
            options.setFilter(filter);

            executeMirroring(mirrorApp, options, logger);
        }
    }

    /**
     * Set filter value so that the feature JAR units and artifacts are included when mirroring.
     */
    private static void addFilterForFeatureJARs(Map<String, String> filter) {
        filter.put("org.eclipse.update.install.features", "true");
    }

    private void executeMirroring(MirrorApplication mirrorApp, SlicingOptions options, MavenLogger logger)
            throws FacadeException {
        try {
            LogListener logListener = new LogListener(logger);
            mirrorApp.setLog(logListener);
            // mirrorApp.setValidate( true ); // TODO Broken; fix at Eclipse

            mirrorApp.setSlicingOptions(options);

            IStatus returnStatus = mirrorApp.run(null);
            checkStatus(returnStatus);
            logListener.showHelpForLoggedMessages();

        } catch (ProvisionException e) {
            throw new FacadeException(MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems(e.getStatus()), e);
        }
    }

    private static void setSourceRepositories(MirrorApplication mirrorApp, RepositoryReferences sources) {
        setSourceRepositories(mirrorApp, sources.getMetadataRepositories(), RepositoryDescriptor.KIND_METADATA);
        setSourceRepositories(mirrorApp, sources.getArtifactRepositories(), RepositoryDescriptor.KIND_ARTIFACT);
    }

    private static void setSourceRepositories(MirrorApplication mirrorApp, Collection<URI> repositoryLocations,
            String repositoryKind) {
        for (URI repositoryLocation : repositoryLocations) {
            RepositoryDescriptor repository = new RepositoryDescriptor();
            repository.setKind(repositoryKind);
            repository.setLocation(repositoryLocation);
            mirrorApp.addSource(repository);
        }
    }

    private static List<IInstallableUnit> toInstallableUnitList(Collection<?> units) {
        List<IInstallableUnit> result = new ArrayList<IInstallableUnit>(units.size());
        for (Object unit : units) {
            result.add((IInstallableUnit) unit);
        }
        return result;
    }

    private static void checkStatus(IStatus status) throws FacadeException {
        if (!status.isOK()) {
            throw new FacadeException(MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems(status),
                    status.getException());
        }
    }

    static class LogListener implements IArtifactMirrorLog {
        private static final String MIRROR_TOOL_MESSAGE_PREFIX = "Mirror tool: ";
        private static final URI MIRROR_TOOL_MESSAGE_HELP = URI
                .create("http://wiki.eclipse.org/Tycho_Messages_Explained#Mirror_tool");

        private final MavenLogger logger;
        private boolean hasLogged = false;

        LogListener(MavenLogger logger) {
            this.logger = logger;
        }

        public void log(IArtifactDescriptor descriptor, IStatus status) {
            if (!status.isOK()) {
                logger.debug(MIRROR_TOOL_MESSAGE_PREFIX + StatusTool.collectProblems(status));
                hasLogged = true;
            }
        }

        public void log(IStatus status) {
            if (!status.isOK()) {
                logger.warn(MIRROR_TOOL_MESSAGE_PREFIX + StatusTool.collectProblems(status));
                hasLogged = true;
            }
        }

        public void showHelpForLoggedMessages() {
            if (hasLogged) {
                logger.warn("More information on the preceding warning(s) can be found here:");
                logger.warn("- " + MIRROR_TOOL_MESSAGE_HELP);
            }

        }

        public void close() {
        }

    }
}
