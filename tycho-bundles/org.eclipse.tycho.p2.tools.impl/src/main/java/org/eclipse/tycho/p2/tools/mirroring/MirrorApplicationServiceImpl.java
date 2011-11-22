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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.mirroring.IArtifactMirrorLog;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.TargetEnvironment;
import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.mirroring.facade.IUDescription;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorOptions;
import org.eclipse.tycho.p2.util.StatusTool;

@SuppressWarnings("restriction")
public class MirrorApplicationServiceImpl implements MirrorApplicationService {

    private static final String MIRROR_FAILURE_MESSAGE = "Mirroring failed";

    private MavenContext mavenContext;

    public void mirrorStandalone(RepositoryReferences sources, DestinationRepositoryDescriptor destination,
            Collection<IUDescription> seedIUs, MirrorOptions mirrorOptions, BuildOutputDirectory tempDirectory)
            throws FacadeException {
        IProvisioningAgent agent = Activator.createProvisioningAgent(tempDirectory);
        try {
            final MirrorApplication mirrorApp = createMirrorApplication(sources, destination, agent);
            mirrorApp.setSlicingOptions(createSlicingOptions(mirrorOptions));
            try {
                // we want to see mirror progress as this is a possibly long-running operation
                mirrorApp.setVerbose(true);
                mirrorApp.setLog(new LogListener(mavenContext.getLogger()));
                mirrorApp.setSourceIUs(querySourceIus(seedIUs, mirrorApp.getCompositeMetadataRepository(), sources));
                IStatus returnStatus = mirrorApp.run(null);
                checkStatus(returnStatus);

            } catch (ProvisionException e) {
                throw new FacadeException(MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems(e.getStatus()), e);
            }
        } finally {
            agent.stop();
        }
    }

    private static SlicingOptions createSlicingOptions(MirrorOptions mirrorOptions) {
        SlicingOptions slicingOptions = new SlicingOptions();
        slicingOptions.considerStrictDependencyOnly(mirrorOptions.isFollowStrictOnly());
        slicingOptions.everythingGreedy(mirrorOptions.isIncludeNonGreedy());
        slicingOptions.followOnlyFilteredRequirements(mirrorOptions.isFollowOnlyFilteredRequirements());
        slicingOptions.includeOptionalDependencies(mirrorOptions.isIncludeOptional());
        slicingOptions.latestVersionOnly(mirrorOptions.isLatestVersionOnly());
        slicingOptions.setFilter(mirrorOptions.getFilter());
        return slicingOptions;
    }

    private List<IInstallableUnit> querySourceIus(Collection<IUDescription> sourceIUs, IMetadataRepository repository,
            RepositoryReferences sources) throws FacadeException {
        if (sourceIUs == null || sourceIUs.isEmpty()) {
            return null;
        }
        List<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
        for (IUDescription iu : sourceIUs) {
            IQuery<IInstallableUnit> iuQuery = createQuery(iu);
            Iterator<IInstallableUnit> queryResult = repository.query(iuQuery, null).iterator();
            if (!queryResult.hasNext()) {
                throw new FacadeException("Could not find IU " + iu.toString() + " in any of the source repositories "
                        + sources.getMetadataRepositories(), null);
            }
            while (queryResult.hasNext()) {
                result.add(queryResult.next());
            }
        }
        return result;
    }

    private IQuery<IInstallableUnit> createQuery(IUDescription iu) {
        String id = iu.getId();
        String version = iu.getVersion();
        if (version == null || version.length() == 0) {
            return QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id));
        } else {
            return QueryUtil.createIUQuery(id, Version.parseVersion(version));
        }
    }

    public void mirrorReactor(RepositoryReferences sources, DestinationRepositoryDescriptor destination,
            Collection<?> seedUnits, BuildContext context, boolean includeAllDependencies) throws FacadeException {
        IProvisioningAgent agent = Activator.createProvisioningAgent(context.getTargetDirectory());
        try {
            final MirrorApplication mirrorApp = createMirrorApplication(sources, destination, agent);

            // mirror scope: seed units...
            mirrorApp.setSourceIUs(toInstallableUnitList(seedUnits));

            // TODO the p2 mirror tool should support mirroring multiple environments at once
            for (TargetEnvironment environment : context.getEnvironments()) {
                SlicingOptions options = new SlicingOptions();
                options.considerStrictDependencyOnly(!includeAllDependencies);
                Map<String, String> filter = options.getFilter();
                addFilterForFeatureJARs(filter);
                filter.putAll(environment.toFilter());
                mirrorApp.setSlicingOptions(options);

                try {
                    LogListener logListener = new LogListener(mavenContext.getLogger());
                    mirrorApp.setLog(logListener);

                    IStatus returnStatus = mirrorApp.run(null);
                    checkStatus(returnStatus);
                    logListener.showHelpForLoggedMessages();

                } catch (ProvisionException e) {
                    throw new FacadeException(
                            MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems(e.getStatus()), e);
                }
            }
        } finally {
            agent.stop();
        }
    }

    private static MirrorApplication createMirrorApplication(RepositoryReferences sources,
            DestinationRepositoryDescriptor destination, IProvisioningAgent agent) {
        final MirrorApplication mirrorApp = new MirrorApplication(agent);

        List<RepositoryDescriptor> sourceDescriptors = createSourceDescriptors(sources);
        for (RepositoryDescriptor sourceDescriptor : sourceDescriptors) {
            mirrorApp.addSource(sourceDescriptor);
        }
        mirrorApp.addDestination(createDestinationDescriptor(destination));

        // mirrorApp.setValidate( true ); // TODO Broken; fix at Eclipse

        return mirrorApp;
    }

    private static RepositoryDescriptor createDestinationDescriptor(DestinationRepositoryDescriptor destination) {
        final RepositoryDescriptor destinationDescriptor = new RepositoryDescriptor();
        destinationDescriptor.setLocation(destination.getLocation().toURI());
        destinationDescriptor.setAppend(destination.isAppend());
        destinationDescriptor.setName(destination.getName());
        destinationDescriptor.setCompressed(destination.isCompress());
        if (destination.isMetaDataOnly()) {
            // only mirror metadata
            destinationDescriptor.setKind(RepositoryDescriptor.KIND_METADATA);
        } else {
            // metadata and artifacts is the default
        }
        return destinationDescriptor;
    }

    /**
     * Set filter value so that the feature JAR units and artifacts are included when mirroring.
     */
    private static void addFilterForFeatureJARs(Map<String, String> filter) {
        filter.put("org.eclipse.update.install.features", "true");
    }

    private static List<RepositoryDescriptor> createSourceDescriptors(RepositoryReferences sources) {
        List<RepositoryDescriptor> result = new ArrayList<RepositoryDescriptor>();
        createSourceRepositories(result, sources.getMetadataRepositories(), RepositoryDescriptor.KIND_METADATA);
        createSourceRepositories(result, sources.getArtifactRepositories(), RepositoryDescriptor.KIND_ARTIFACT);
        return result;
    }

    private static void createSourceRepositories(List<RepositoryDescriptor> result,
            Collection<URI> repositoryLocations, String repositoryKind) {
        for (URI repositoryLocation : repositoryLocations) {
            RepositoryDescriptor repository = new RepositoryDescriptor();
            repository.setKind(repositoryKind);
            repository.setLocation(repositoryLocation);
            result.add(repository);
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
        if (status.matches(IStatus.ERROR)) {
            throw new FacadeException(MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems(status),
                    StatusTool.findException(status));
        }
    }

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
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
