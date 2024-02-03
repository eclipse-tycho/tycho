/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat, Inc. - fragment creation, Bug 460967
 *******************************************************************************/
package org.eclipse.tycho.p2tools.copiedfromp2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.engine.DownloadManager;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitPhase;
import org.eclipse.equinox.internal.p2.engine.Phase;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.p2.engine.ProfileWriter;
import org.eclipse.equinox.internal.p2.engine.ProfileXMLConstants;
import org.eclipse.equinox.internal.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorImpl;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;

/**
 * The transformer takes an existing p2 repository (local or remote), iterates over its list of IUs,
 * and fetches all of the corresponding artifacts to a user-specified location. Once fetched, the
 * artifacts will be in "runnable" form... that is directory-based bundles will be extracted into
 * folders.
 *
 * @since 1.0
 */
@SuppressWarnings("nls")
public class Repo2Runnable extends AbstractApplication implements IApplication {

    public Repo2Runnable(IProvisioningAgent agent) {
        super(agent);
    }

    private static final String NATIVE_ARTIFACTS = "nativeArtifacts"; //$NON-NLS-1$
    private static final String NATIVE_TYPE = "org.eclipse.equinox.p2.native"; //$NON-NLS-1$
    private static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
    private static final String PARM_PROFILE = "profile"; //$NON-NLS-1$

    private boolean createFragments;
    private boolean flagAsRunnable = false;

    protected class CollectNativesAction extends ProvisioningAction {
        @Override
        public IStatus execute(Map<String, Object> parameters) {
            InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);
            IInstallableUnit installableUnit = operand.second();

            IArtifactRepositoryManager manager = getArtifactRepositoryManager();
            Collection<IArtifactKey> toDownload = installableUnit.getArtifacts();
            if (toDownload == null)
                return Status.OK_STATUS;

            @SuppressWarnings("unchecked")
            List<IArtifactRequest> artifactRequests = (List<IArtifactRequest>) parameters.get(NATIVE_ARTIFACTS);

            IProfile profile = (IProfile) parameters.get(PARM_PROFILE);
            String statsParameter = null;
            if (profile != null)
                statsParameter = profile.getProperty(IProfile.PROP_STATS_PARAMETERS);

            for (IArtifactKey keyToDownload : toDownload) {
                IArtifactRequest request = manager.createMirrorRequest(keyToDownload, destinationArtifactRepository,
                        null, null, statsParameter);
                artifactRequests.add(request);
            }
            return Status.OK_STATUS;
        }

        @Override
        public IStatus undo(Map<String, Object> parameters) {
            // nothing to do for now
            return Status.OK_STATUS;
        }
    }

    protected class CollectNativesPhase extends InstallableUnitPhase {
        public CollectNativesPhase(int weight) {
            super(NATIVE_ARTIFACTS, weight);
        }

        @Override
        protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
            IInstallableUnit unit = operand.second();
            if (unit.getTouchpointType().getId().equals(NATIVE_TYPE)) {
                return Collections.singletonList(new CollectNativesAction());
            }
            return null;
        }

        @Override
        protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
            parameters.put(NATIVE_ARTIFACTS, new ArrayList<>());
            return null;
        }

        @Override
        protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
            @SuppressWarnings("unchecked")
            List<IArtifactRequest> artifactRequests = (List<IArtifactRequest>) parameters.get(NATIVE_ARTIFACTS);
            ProvisioningContext context = (ProvisioningContext) parameters.get(PARM_CONTEXT);
            IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
            DownloadManager dm = new DownloadManager(context, agent);
            for (IArtifactRequest request : artifactRequests) {
                dm.add(request);
            }
            return dm.start(monitor);
        }
    }

    // the list of IUs that we actually transformed... could have come from the repo
    // or have been user-specified.
    private Collection<IInstallableUnit> processedIUs = new ArrayList<>();

    /*
     * Perform the transformation.
     */
    @Override
    public IStatus run(IProgressMonitor monitor) throws ProvisionException {
        SubMonitor progress = SubMonitor.convert(monitor, 5);

        initializeRepos(progress);

        // ensure all the right parameters are set
        validate();

        // figure out which IUs we need to process
        collectIUs(progress.newChild(1));

        // call the engine with only the "collect" phase so all we do is download
        IProfile profile = createProfile();
        try {
            IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
            if (engine == null)
                throw new ProvisionException(Messages.exception_noEngineService);
            ProvisioningContext context = new ProvisioningContext(agent);
            context.setMetadataRepositories(getRepositories(true));
            context.setArtifactRepositories(getRepositories(false));
            IProvisioningPlan plan = engine.createPlan(profile, context);
            for (IInstallableUnit iu : processedIUs) {
                plan.addInstallableUnit(iu);
            }
            IStatus result = engine.perform(plan, getPhaseSet(), progress.newChild(1));
            PhaseSet nativeSet = getNativePhase();
            if (nativeSet != null)
                engine.perform(plan, nativeSet, progress.newChild(1));

            // publish the metadata to a destination - if requested
            publishMetadata(progress.newChild(1));

            setRunnableProperty(destinationArtifactRepository);
            // return the resulting status

            if (createFragments) {
                File parentDir = new File(destinationArtifactRepository.getLocation().toString().substring(5));
                File pluginsDir = new File(parentDir, "plugins");
                File fragmentInfo = new File(parentDir, "fragment.info");
                HashSet<BundleInfo> bundles = new HashSet<>();
                try {
                    for (IInstallableUnit iu : processedIUs) {
                        if (iu.getId().equals("a.jre"))
                            continue;
                        Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
                        for (IProvidedCapability cap : providedCapabilities) {
                            if ("org.eclipse.equinox.p2.eclipse.type".equals(cap.getNamespace())) {
                                if ("bundle".equals(cap.getName())) {
                                    File candidate = new File(pluginsDir, iu.getId() + "_" + iu.getVersion());
                                    if (candidate.exists()) {
                                        bundles.add(new BundleInfo(iu.getId(), iu.getVersion().toString(),
                                                candidate.toURI(), 4, false));
                                    }
                                    candidate = new File(pluginsDir, iu.getId() + "_" + iu.getVersion() + ".jar");
                                    if (candidate.exists()) {
                                        bundles.add(new BundleInfo(iu.getId(), iu.getVersion().toString(),
                                                candidate.toURI(), 4, false));
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    SimpleConfiguratorManipulator simpleManipulator = new SimpleConfiguratorManipulatorImpl();
                    simpleManipulator.saveConfiguration(bundles.toArray(new BundleInfo[0]), fragmentInfo,
                            parentDir.toURI());
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return result;
        } finally {
            // cleanup by removing the temporary profile and unloading the repos which were new
            removeProfile(profile);
            finalizeRepositories();
        }
    }

    static class Writer extends ProfileWriter {

        public Writer(OutputStream output) {
            super(output, new ProcessingInstruction[] { ProcessingInstruction
                    .makeTargetVersionInstruction(PROFILE_TARGET, ProfileXMLConstants.CURRENT_VERSION) });
        }
    }

    private void setRunnableProperty(IArtifactRepository destinationArtifactRepository) {
        if (flagAsRunnable)
            destinationArtifactRepository.setProperty(IArtifactRepository.PROP_RUNNABLE, Boolean.TRUE.toString(),
                    new NullProgressMonitor());
    }

    protected URI[] getRepositories(boolean metadata) {
        List<URI> repos = new ArrayList<>();
        for (RepositoryDescriptor repo : sourceRepositories) {
            if (metadata ? repo.isMetadata() : repo.isArtifact())
                repos.add(repo.getRepoLocation());
        }
        return repos.toArray(new URI[repos.size()]);
    }

    protected PhaseSet getPhaseSet() {
        return new PhaseSet(new Phase[] { new Collect(100) }) {
            /* nothing to override */};
    }

    protected PhaseSet getNativePhase() {
        return new PhaseSet(new Phase[] { new CollectNativesPhase(100) }) {
            /* nothing to override */};
    }

    /*
     * Figure out exactly which IUs we have to process.
     */
    private void collectIUs(IProgressMonitor monitor) throws ProvisionException {
        // if the user told us exactly which IUs to process, then just set it and return.
        if (sourceIUs != null && !sourceIUs.isEmpty()) {
            processedIUs = sourceIUs;
            return;
        }
        // get all IUs from the repos
        if (!hasMetadataSources())
            throw new ProvisionException(Messages.exception_needIUsOrNonEmptyRepo);

        Iterator<IInstallableUnit> itor = getAllIUs(getCompositeMetadataRepository(), monitor).iterator();
        while (itor.hasNext())
            processedIUs.add(itor.next());

        if (processedIUs.isEmpty())
            throw new ProvisionException(Messages.exception_needIUsOrNonEmptyRepo);
    }

    /*
     * If there is a destination metadata repository set, then add all our transformed IUs to it.
     */
    private void publishMetadata(IProgressMonitor monitor) {
        // publishing the metadata is optional
        if (destinationMetadataRepository == null)
            return;
        destinationMetadataRepository.addInstallableUnits(processedIUs);
    }

    /*
     * Return a collector over all the IUs contained in the given repository.
     */
    private IQueryResult<IInstallableUnit> getAllIUs(IMetadataRepository repository, IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, 2);
        try {
            return repository.query(QueryUtil.createIUAnyQuery(), progress.newChild(1));
        } finally {
            progress.done();
        }
    }

    /*
     * Remove the given profile from the profile registry.
     */
    private void removeProfile(IProfile profile) throws ProvisionException {
        IProfileRegistry registry = agent.getService(IProfileRegistry.class);
        registry.removeProfile(profile.getProfileId());
    }

    /*
     * Create and return a new profile.
     */
    private IProfile createProfile() throws ProvisionException {
        Map<String, String> properties = new HashMap<>();
        properties.put(IProfile.PROP_CACHE,
                URIUtil.toFile(destinationArtifactRepository.getLocation()).getAbsolutePath());
        properties.put(IProfile.PROP_INSTALL_FOLDER,
                URIUtil.toFile(destinationArtifactRepository.getLocation()).getAbsolutePath());
        IProfileRegistry registry = agent.getService(IProfileRegistry.class);
        return registry.addProfile(System.currentTimeMillis() + "-" + Math.random(), properties); //$NON-NLS-1$
    }

    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        processCommandLineArgs(args);
        // perform the transformation
        run(null);
        return IApplication.EXIT_OK;
    }

    /*
     * Iterate over the command-line arguments and prepare the transformer for processing.
     */
    private void processCommandLineArgs(String[] args) throws URISyntaxException {
        if (args == null)
            return;
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            String arg = null;
            if (i != args.length - 1 && !args[i + 1].startsWith("-")) { //$NON-NLS-1$
                arg = args[++i];
            }

            if (option.equalsIgnoreCase("-source")) { //$NON-NLS-1$
                RepositoryDescriptor source = new RepositoryDescriptor();
                source.setLocation(URIUtil.fromString(arg));
                addSource(source);
            }

            if (option.equalsIgnoreCase("-destination")) { //$NON-NLS-1$
                RepositoryDescriptor destination = new RepositoryDescriptor();
                destination.setLocation(URIUtil.fromString(arg));
                addDestination(destination);
            }

            if (option.equalsIgnoreCase("-flagAsRunnable")) { //$NON-NLS-1$
                setFlagAsRunnable(true);
            }

            if (option.equalsIgnoreCase("-createFragments")) { //$NON-NLS-1$
                setCreateFragments(true);
            }
        }
    }

    public void setFlagAsRunnable(boolean runnable) {
        flagAsRunnable = runnable;
    }

    /*
     * Ensure all mandatory parameters have been set. Throw an exception if there are any missing.
     * We don't require the user to specify the artifact repository here, we will default to the
     * ones already registered in the manager. (callers are free to add more if they wish)
     */
    private void validate() throws ProvisionException {
        if (!hasMetadataSources() && sourceIUs == null)
            throw new ProvisionException(Messages.exception_needIUsOrNonEmptyRepo);
        if (destinationArtifactRepository == null)
            throw new ProvisionException(Messages.exception_needDestinationRepo);
    }

    @Override
    public void stop() {
        // nothing to do
    }

    public void setCreateFragments(boolean createFragments) {
        this.createFragments = createFragments;

    }
}
