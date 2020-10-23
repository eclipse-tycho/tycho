/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MultiLineLogger;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;
import org.eclipse.tycho.repository.util.DuplicateFilteringLoggingProgressMonitor;
import org.eclipse.tycho.repository.util.LoggingProgressMonitor;
import org.eclipse.tycho.repository.util.StatusTool;

/**
 * {@link IRawArtifactFileProvider} which caches all accessed artifacts in the local Maven
 * repository.
 * 
 * <p>
 * Note that a <tt>MirroringArtifactProvider</tt> is not a transparent cache of the remote
 * providers. The content provided by this instance differs from the remote providers' content in
 * the following ways:
 * <ul>
 * <li>This instance provides all content in the local Maven repository (previously cached or
 * installed) in addition to the remote content. This allows lazy access to the remote repositories
 * (bug 347477).</li>
 * <li>This instance only provides the remote artifacts in certain formats, i.e. only the canonical
 * format, or the canonical format and the packed format.</li>
 * </ul>
 * </p>
 */
public class MirroringArtifactProvider implements IRawArtifactFileProvider {

    protected final MavenLogger logger;
    protected final MavenLogger splittingLogger;

    protected final IRawArtifactProvider remoteProviders;
    protected final LocalArtifactRepository localArtifactRepository;

    protected final IProgressMonitor monitor;

    /**
     * Creates a new {@link MirroringArtifactProvider} instance.
     * 
     * @param localArtifactRepository
     *            The local Maven repository
     * @param remoteProviders
     *            The provider that will be queried by this instance when it is asked for an
     *            artifact which is not (yet) available in the local Maven repository. Typically
     *            this provider is backed by remote p2 repositories.
     * @param mirrorPacked
     *            If <code>true</code>, the returned instance will also mirror the packed format of
     *            all artifacts it is asked for.
     * @param logger
     *            a logger for progress output
     */
    public static MirroringArtifactProvider createInstance(LocalArtifactRepository localArtifactRepository,
            IRawArtifactProvider remoteProviders, boolean mirrorPacked, MavenLogger logger) {
        if (!mirrorPacked) {
            return new MirroringArtifactProvider(localArtifactRepository, remoteProviders, logger);
        } else {
            return new PackedFormatMirroringArtifactProvider(localArtifactRepository, remoteProviders, logger);
        }
    }

    MirroringArtifactProvider(LocalArtifactRepository localArtifactRepository, IRawArtifactProvider remoteProviders,
            MavenLogger logger) {
        this.remoteProviders = remoteProviders;
        this.localArtifactRepository = localArtifactRepository;
        this.logger = logger;
        this.splittingLogger = new MultiLineLogger(logger);
        this.monitor = new LoggingProgressMonitor(logger);
    }

    // pass through methods

    @Override
    public final boolean contains(IArtifactKey key) {
        if (localArtifactRepository.contains(key)) {
            return true;
        }
        return remoteProviders.contains(key);
    }

    @Override
    @SuppressWarnings({ "restriction", "unchecked" })
    public final IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        IQueryable<IArtifactKey>[] sources = new IQueryable[] { localArtifactRepository, remoteProviders };
        return new CompoundQueryable<>(sources).query(query, nonNull(monitor));
    }

    // mirroring methods

    @Override
    public final File getArtifactFile(IArtifactKey key) throws MirroringFailedException {
        if (makeLocallyAvailable(key)) {
            return localArtifactRepository.getArtifactFile(key);
        }
        return null;
    }

    @Override
    public final File getArtifactFile(IArtifactDescriptor descriptor) throws MirroringFailedException {
        if (makeLocallyAvailable(descriptor.getArtifactKey())) {
            return localArtifactRepository.getArtifactFile(descriptor);
        }
        return null;
    }

    @Override
    public final IStatus getArtifact(IArtifactSink sink, IProgressMonitor monitor)
            throws ArtifactSinkException, MirroringFailedException {
        IArtifactKey requestedKey = sink.getArtifactToBeWritten();
        if (makeLocallyAvailable(requestedKey)) {
            return localArtifactRepository.getArtifact(sink, monitor);
        }
        return artifactNotFoundStatus(requestedKey);
    }

    @Override
    public final IStatus getRawArtifact(IRawArtifactSink sink, IProgressMonitor monitor)
            throws ArtifactSinkException, MirroringFailedException {
        IArtifactKey requestedKey = sink.getArtifactToBeWritten();
        if (makeLocallyAvailable(requestedKey)) {
            return localArtifactRepository.getRawArtifact(sink, monitor);
        }
        return artifactNotFoundStatus(requestedKey);
    }

    @Override
    public final IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) throws MirroringFailedException {
        if (makeLocallyAvailable(key)) {
            return localArtifactRepository.getArtifactDescriptors(key);
        }
        return new IArtifactDescriptor[0];
    }

    @Override
    public final boolean contains(IArtifactDescriptor descriptor) throws MirroringFailedException {
        if (makeLocallyAvailable(descriptor.getArtifactKey())) {
            return localArtifactRepository.contains(descriptor);
        }
        return false;
    }

    /**
     * Downloads the artifact from remote if it isn't available locally yet.
     * 
     * @return <code>false</code> if the artifact is neither already cached locally nor available
     *         remotely.
     * @throws MirroringFailedException
     *             if a fatal error occurred while downloading the artifact.
     */
    private boolean makeLocallyAvailable(IArtifactKey key) throws MirroringFailedException {
        // TODO 397355 cache artifactDescriptors for the key so that only one synchronization is necessary to figure out if a download is needed
        try {
            boolean isAvailable = makeOneFormatLocallyAvailable(key);

            if (isAvailable) {
                /*
                 * Always execute this step (even when not mirroring packed artifacts) so that a
                 * local repository that only contains the packed format of an artifact is
                 * automatically corrected. We need the canonical format in the local repository so
                 * that {@link IArtifactFileProvider#getArtifactFile(IArtifactKey)} does not return
                 * null.
                 */
                ensureArtifactIsPresentInCanonicalFormat(key);
            }
            return isAvailable;

        } catch (ProvisionException e) {
            throw new MirroringFailedException(
                    "Error while mirroring artifact " + key + " to the local Maven repository" + e.getMessage(), e);
        } catch (ArtifactSinkException e) {
            throw new MirroringFailedException(
                    "Error while mirroring artifact " + key + " to the local Maven repository" + e.getMessage(), e);
        }
    }

    protected boolean makeOneFormatLocallyAvailable(IArtifactKey key)
            throws MirroringFailedException, ProvisionException, ArtifactSinkException {

        if (localArtifactRepository.contains(key)) {
            return true;
        } else if (remoteProviders.contains(key)) {
            downloadArtifact(key);
            return true;
        } else {
            return false;
        }
    }

    protected final void downloadArtifact(IArtifactKey key)
            throws MirroringFailedException, ProvisionException, ArtifactSinkException {

//        logger.info("Downloading " + key.getId() + "_" + key.getVersion() + "..."); // p2 output is enough
        IStatus transferStatus = downloadMostSpecificNeededFormatOfArtifact(key);

        if (transferStatus.matches(IStatus.ERROR | IStatus.CANCEL)) {
            splittingLogger.error(StatusTool.toLogMessage(transferStatus));
            throw new MirroringFailedException("Could not mirror artifact " + key + " into the local Maven repository."
                    + "See log output for details.", StatusTool.findException(transferStatus));
        } else if (transferStatus.matches(IStatus.WARNING)) {
            splittingLogger.warn(StatusTool.toLogMessage(transferStatus));
        }

    }

    protected IStatus downloadMostSpecificNeededFormatOfArtifact(IArtifactKey key)
            throws ProvisionException, ArtifactSinkException {
        // only need canonical format
        return downloadCanonicalArtifact(key);
    }

    protected final IStatus downloadCanonicalArtifact(IArtifactKey key)
            throws ProvisionException, ArtifactSinkException {
        // TODO 397355 ignore ProvisionException.ARTIFACT_EXISTS - artifact may have been added by other thread in the meantime
        IArtifactSink localSink = localArtifactRepository.newAddingArtifactSink(key);
        return remoteProviders.getArtifact(localSink, monitorForDownload());
    }

    private void ensureArtifactIsPresentInCanonicalFormat(IArtifactKey key)
            throws ProvisionException, ArtifactSinkException {
        if (findCanonicalDescriptor(localArtifactRepository.getArtifactDescriptors(key)) == null) {
            boolean isPack200able = Runtime.version().feature() <= 13;
            if (isPack200able) {
                // there was at least format available, but not the canonical format -> create it from the packed format 
                createCanonicalArtifactFromLocalPackedArtifact(key);
            } else {
                downloadCanonicalArtifact(key);
            }
        }
    }

    private void createCanonicalArtifactFromLocalPackedArtifact(IArtifactKey key)
            throws ProvisionException, ArtifactSinkException {
        logger.info("Unpacking " + key.getId() + "_" + key.getVersion() + "...");

        // TODO 397355 ignore ProvisionException.ARTIFACT_EXISTS
        IArtifactSink sink = localArtifactRepository.newAddingArtifactSink(key);
        localArtifactRepository.getArtifact(sink, monitor);
    }

    static IArtifactDescriptor findPackedDescriptor(IArtifactDescriptor[] descriptors) {
        for (IArtifactDescriptor descriptor : descriptors) {
            if (ArtifactTransferPolicy.isPack200Format(descriptor)) {
                return descriptor;
            }
        }
        return null;
    }

    static IArtifactDescriptor findCanonicalDescriptor(IArtifactDescriptor[] descriptors) {
        for (IArtifactDescriptor descriptor : descriptors) {
            if (ArtifactTransferPolicy.isCanonicalFormat(descriptor)) {
                return descriptor;
            }
        }
        return null;
    }

    private static IStatus artifactNotFoundStatus(IArtifactKey key) {
        return new Status(IStatus.ERROR, BUNDLE_ID, ProvisionException.ARTIFACT_NOT_FOUND, "Artifact " + key
                + " is neither available in the local Maven repository nor in the configured remote repositories",
                null);
    }

    /**
     * Returns an {@link IProgressMonitor} which translates p2's status updates into a reasonable
     * log output.
     */
    final IProgressMonitor monitorForDownload() {
        // create a new instance for each call - DuplicateFilteringLoggingProgressMonitor is not thread-safe
        return new DuplicateFilteringLoggingProgressMonitor(logger);
    }

    private static IProgressMonitor nonNull(IProgressMonitor monitor) {
        if (monitor == null)
            return new NullProgressMonitor();
        else
            return monitor;
    }

    public static class MirroringFailedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        MirroringFailedException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    @Override
    public boolean isFileAlreadyAvailable(IArtifactKey artifactKey) {
        return localArtifactRepository.contains(artifactKey);
    }
}
