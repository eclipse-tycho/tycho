/*******************************************************************************
 * Copyright (c) 2012, 2023 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *    Christoph Läubrich    - Issue #658 - Tycho strips p2 artifact properties (eg PGP, maven info...)
 *                          - Issue #692 - Check Hashsums for local cached artifacts
 *******************************************************************************/
package org.eclipse.tycho.p2.repository;

import static org.eclipse.tycho.p2.repository.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.codec.digest.DigestUtils;
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
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactSinkException;
import org.eclipse.tycho.IArtifactSink;
import org.eclipse.tycho.IRawArtifactFileProvider;
import org.eclipse.tycho.IRawArtifactProvider;
import org.eclipse.tycho.IRawArtifactSink;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.DuplicateFilteringLoggingProgressMonitor;
import org.eclipse.tycho.core.shared.LoggingProgressMonitor;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenContext.ChecksumPolicy;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MultiLineLogger;
import org.eclipse.tycho.core.shared.StatusTool;

/**
 * {@link IRawArtifactFileProvider} which caches all accessed artifacts in the local Maven
 * repository.
 *
 * <p>
 * Note that a <code>MirroringArtifactProvider</code> is not a transparent cache of the remote
 * providers. The content provided by this instance differs from the remote providers' content in
 * the following ways:
 * <ul>
 * <li>This instance provides all content in the local Maven repository (previously cached or
 * installed) in addition to the remote content. This allows lazy access to the remote repositories
 * (bug 347477).</li>
 * <li>This instance only provides the remote artifacts in certain formats, i.e. only the canonical
 * format.</li>
 * </ul>
 * </p>
 */
public class MirroringArtifactProvider implements IRawArtifactFileProvider {

    protected final MavenLogger logger;
    protected final MavenLogger splittingLogger;

    protected final IRawArtifactProvider remoteProviders;
    protected final LocalArtifactRepository localArtifactRepository;

    protected final IProgressMonitor monitor;
    private MavenContext mavenContext;
    private IArtifactRepository shaddowRepository;

    /**
     * Creates a new {@link MirroringArtifactProvider} instance.
     *
     * @param localArtifactRepository
     *            The local Maven repository
     * @param remoteProviders
     *            The provider that will be queried by this instance when it is asked for an
     *            artifact which is not (yet) available in the local Maven repository. Typically
     *            this provider is backed by remote p2 repositories.
     * @param logger
     *            a logger for progress output
     */
    public static MirroringArtifactProvider createInstance(LocalArtifactRepository localArtifactRepository,
            IRawArtifactProvider remoteProviders, MavenContext context) {
        return new MirroringArtifactProvider(localArtifactRepository, remoteProviders, context);
    }

    MirroringArtifactProvider(LocalArtifactRepository localArtifactRepository, IRawArtifactProvider remoteProviders,
            MavenContext mavenContext) {
        this.remoteProviders = remoteProviders;
        this.localArtifactRepository = localArtifactRepository;
        this.mavenContext = mavenContext;
        this.logger = mavenContext.getLogger();
        this.splittingLogger = new MultiLineLogger(this.logger);
        this.monitor = new LoggingProgressMonitor(this.logger);
        this.shaddowRepository = new ProviderOnlyArtifactRepository(this,
                localArtifactRepository.getProvisioningAgent(), null);
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
    @SuppressWarnings({ "unchecked" })
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
        if (localArtifactRepository.contains(key)) {
            return localArtifactRepository.getArtifactDescriptors(key);
        }
        if (remoteProviders.contains(key)) {
            //we can use it if the remote contains it, but want to shadow the repository returned by the descriptor
            //just in case someone uses this to download an artifact we are asked again...
            return Arrays.stream(remoteProviders.getArtifactDescriptors(key))
                    .filter(ArtifactTransferPolicy::isCanonicalFormat)
                    .map(base -> new MirrorArtifactDescriptor(base, this)).toArray(IArtifactDescriptor[]::new);
        }
        return new IArtifactDescriptor[0];
    }

    @Override
    public final boolean contains(IArtifactDescriptor descriptor) throws MirroringFailedException {
        if (descriptor instanceof MirrorArtifactDescriptor mirrorDescriptor) {
            return mirrorDescriptor.provider == this;
        }
        //we can use this if the local or the remote contains this!
        return localArtifactRepository.contains(descriptor) || remoteProviders.contains(descriptor);
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
        try {
            boolean isAvailable = makeOneFormatLocallyAvailable(key);

            if (isAvailable) {
                /*
                 * Always execute this step so that a local repository that only contains the packed
                 * format of an artifact is automatically corrected. We need the canonical format in
                 * the local repository so that {@link
                 * IArtifactFileProvider#getArtifactFile(IArtifactKey)} does not return null.
                 */
                ensureArtifactIsPresentInCanonicalFormat(key);
            }
            return isAvailable;

        } catch (ProvisionException e) {
            if (ProvisionException.ARTIFACT_EXISTS == e.getStatus().getCode()) {
                //there is some race condition here so we need to start over again
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e1) {
                }
                return makeLocallyAvailable(key);
            }
            throw new MirroringFailedException(
                    "Error while mirroring artifact " + key + " to the local Maven repository" + e.getMessage(), e);
        } catch (ArtifactSinkException e) {
            throw new MirroringFailedException(
                    "Error while mirroring artifact " + key + " to the local Maven repository" + e.getMessage(), e);
        }
    }

    protected boolean makeOneFormatLocallyAvailable(IArtifactKey key)
            throws MirroringFailedException, ProvisionException, ArtifactSinkException {

        if (isFileAlreadyAvailable(key)) {
            return true;
        } else if (remoteProviders.contains(key)) {
            Lock downloadLock = localArtifactRepository.getLockForDownload(key);
            downloadLock.lock();
            try {
                if (!isFileAlreadyAvailable(key)) { // check again within lock
                    File artifactFile;
                    if (localArtifactRepository.contains(key)) {
                        artifactFile = localArtifactRepository.getArtifactFile(key);
                        localArtifactRepository.removeDescriptor(key);
                    } else {
                        artifactFile = localArtifactRepository.internalGetArtifactStorageLocation(
                                localArtifactRepository.createArtifactDescriptor(key));
                    }
                    if (artifactFile != null && artifactFile.isFile()) {
                        //check if only properties has changed...
                        GAVArtifactDescriptor descriptor = newLocalDescriptor(key);
                        if (fileMatchesProperties(artifactFile, descriptor.getProperties(),
                                mavenContext.getChecksumsMode() == ChecksumPolicy.STRICT)) {
                            localArtifactRepository.internalAddDescriptor(descriptor);
                            localArtifactRepository.save();
                            return true;
                        }
                    }
                    downloadArtifact(key);
                    localArtifactRepository.save();
                }
            } finally {
                downloadLock.unlock();
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean fileMatchesProperties(File file, Map<String, String> properties, boolean logFailure) {
        String downloadSize = properties.get("download.size");
        if (downloadSize != null && !downloadSize.equals(String.valueOf(file.length()))) {
            //early break out...
            if (logFailure) {
                mavenContext.getLogger().warn("File size for " + file.getAbsolutePath()
                        + " does not match, attempting to download file again...");
            }
            return false;
        }
        String sha256 = properties.get("download.checksum.sha-256");
        if (sha256 != null) {
            try (FileInputStream stream = new FileInputStream(file)) {
                String fileSha256 = DigestUtils.sha256Hex(stream);
                if (fileSha256.equalsIgnoreCase(sha256)) {
                    return true;
                }
            } catch (IOException e) {
                mavenContext.getLogger().debug("Computing hash sum failed, assume file is corrupted (" + e + ")");
            }
            if (logFailure) {
                mavenContext.getLogger().warn("sha-256 checksum for " + file.getAbsolutePath()
                        + " does not match, attempting to download file again...");
            }
            return false;
        }
        String md5 = properties.get("download.checksum.md5");
        if (md5 != null) {
            try (FileInputStream stream = new FileInputStream(file)) {
                String fileMd5 = DigestUtils.md5Hex(stream);
                if (fileMd5.equalsIgnoreCase(md5)) {
                    return true;
                }
            } catch (IOException e) {
                mavenContext.getLogger().debug("Computing hash sum failed, assume file is corrupted (" + e + ")");
            }
            if (logFailure) {
                mavenContext.getLogger().warn("md5 checksum for " + file.getAbsolutePath()
                        + " does not match, attempting to download file again...");
            }
            return false;
        }
        return false;
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
        GAVArtifactDescriptor localDescriptor = newLocalDescriptor(key);
        IArtifactSink localSink = localArtifactRepository.newAddingArtifactSink(localDescriptor);
        return remoteProviders.getArtifact(localSink, monitorForDownload());
    }

    protected GAVArtifactDescriptor newLocalDescriptor(IArtifactKey key) {
        GAVArtifactDescriptor localDescriptor = localArtifactRepository.createArtifactDescriptor(key);
        IArtifactDescriptor remoteDescriptor = findCanonicalDescriptor(remoteProviders.getArtifactDescriptors(key));
        Map<String, String> properties = getProperties(remoteDescriptor);
        properties.forEach(localDescriptor::setProperty);
        return localDescriptor;
    }

    private void ensureArtifactIsPresentInCanonicalFormat(IArtifactKey key)
            throws ProvisionException, ArtifactSinkException {
        if (findCanonicalDescriptor(localArtifactRepository.getArtifactDescriptors(key)) == null) {
            downloadCanonicalArtifact(key);
        }
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

    private static final class MirrorArtifactDescriptor extends ArtifactDescriptor {

        final MirroringArtifactProvider provider;
        private IArtifactDescriptor base;

        MirrorArtifactDescriptor(IArtifactDescriptor base, MirroringArtifactProvider provider) {
            super(base);
            this.base = base;
            this.provider = provider;
            setRepository(provider.shaddowRepository);
        }

        @Override
        public boolean equals(Object obj) {
            boolean equals = super.equals(obj);
            if (!equals) {
                return base.equals(obj);
            }
            return equals;
        }

        @Override
        public int hashCode() {
            return base.hashCode();
        }

    }

    @Override
    public boolean isFileAlreadyAvailable(IArtifactKey artifactKey) {
        if (localArtifactRepository.contains(artifactKey)) {
            if (mavenContext.isOffline()) {
                return true;
            }
            if (remoteProviders.contains(artifactKey)) {
                //we must compare remote versus local!
                IArtifactDescriptor[] remoteDescriptors = remoteProviders.getArtifactDescriptors(artifactKey);
                IArtifactDescriptor remoteDescriptor = findCanonicalDescriptor(remoteDescriptors);
                IArtifactDescriptor[] localDescriptors = localArtifactRepository.getArtifactDescriptors(artifactKey);
                GAVArtifactDescriptor localDescriptor = (GAVArtifactDescriptor) findCanonicalDescriptor(
                        localDescriptors);
                if (mavenContext.getChecksumsMode() == ChecksumPolicy.STRICT && remoteDescriptor != null
                        && localDescriptor != null) {
                    File artifactFile = localArtifactRepository.getArtifactFile(localDescriptor);
                    if (artifactFile != null && artifactFile.isFile()) {
                        if (!fileMatchesProperties(artifactFile, remoteDescriptor.getProperties(), false)) {
                            return false;
                        }
                    }
                }
                if (remoteDescriptor != null && localDescriptor != null && localDescriptor.getMavenCoordinates()
                        .getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX)) {
                    Map<String, String> remoteProperties = getProperties(remoteDescriptor);
                    Map<String, String> localProperties = getProperties(localDescriptor);
                    if (!Objects.equals(remoteProperties, localProperties)) {
                        if (logger.isExtendedDebugEnabled()) {
                            logger.info("ArtifactKey " + artifactKey + " differs between local and remote:");
                            TreeSet<String> allKeys = new TreeSet<>();
                            allKeys.addAll(remoteProperties.keySet());
                            allKeys.addAll(localProperties.keySet());
                            for (String key : allKeys) {
                                String remoteValue = remoteProperties.get(key);
                                String localValue = localProperties.get(key);
                                if (Objects.equals(remoteValue, localValue)) {
                                    continue;
                                }
                                logger.info(
                                        "\t" + key + " diverged, remote = " + remoteValue + ", local = " + localValue);
                            }
                        }
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private Map<String, String> getProperties(IArtifactDescriptor descriptor) {
        if (descriptor == null) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>(descriptor.getProperties());
        //fix bad metadata in p2...
        if (ArtifactTransferPolicy.isCanonicalFormat(descriptor)
                && "pack200".equals(map.get(TychoConstants.PROP_CLASSIFIER))) {
            map.remove(TychoConstants.PROP_CLASSIFIER);
            map.put(TychoConstants.PROP_EXTENSION, "jar");
            map.put(TychoConstants.PROP_TYPE, PackagingType.TYPE_ECLIPSE_PLUGIN);
        }
        return map;
    }

}
