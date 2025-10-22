/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRingCollection;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.tycho.p2maven.transport.TransportCacheConfig;

@Named
@Singleton
public class PGPService {

    //See GpgSigner.SIGNATURE_EXTENSION
    private static final String SIGNATURE_EXTENSION = ".asc";

    public static final String MAVEN_CENTRAL_KEY_SERVER = "http://pgp.mit.edu/pks/lookup?op=get&search={0}";
    public static final String UBUNTU_KEY_SERVER = "https://keyserver.ubuntu.com/pks/lookup?op=get&search={0}";

    @Inject
    Logger logger;

    @Inject
    RepositorySystem repositorySystem;

    @Inject
    TransportCacheConfig transportCacheConfig;

    /**
     * Get the attached PGP signature for the given MavenProject
     * 
     * @param mavenProject
     * @return the file to the signature or null if no signature is currently attached
     */
    public File getAttachedSignature(MavenProject mavenProject) {
        org.apache.maven.artifact.Artifact artifact = mavenProject.getArtifact();
        if (artifact != null) {
            String attachedSignature = artifact.getArtifactHandler().getExtension() + SIGNATURE_EXTENSION;
            for (var attached : mavenProject.getAttachedArtifacts()) {
                if (attached.getType().equals(attachedSignature)) {
                    //check that this is the "main" artifact signature
                    if (Objects.equals(attached.getArtifactId(), artifact.getArtifactId())
                            && Objects.equals(attached.getGroupId(), artifact.getGroupId())
                            && (attached.getClassifier() == null || attached.getClassifier().isEmpty())) {
                        return attached.getFile();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Fetches the public key for the given id from the provided key server using the supplied retry
     * count
     * 
     * @param keyID
     * @param keyServerUrl
     * @param session
     * @param keyServerRetry
     * @return the public key or <code>null</code> if the server does not provide such a signature
     * @throws IOException
     * @throws PGPException
     */
    public PGPPublicKeyRing getPublicKey(long keyID, String keyServerUrl, int keyServerRetry)
            throws IOException, PGPException {
        String hexKey = "0x" + Long.toHexString(keyID).toUpperCase();
        logger.info("Fetching PGP key with id " + hexKey);
        File keyCacheFile = new File(new File(transportCacheConfig.getCacheLocation(), "pgpkeys"), hexKey + ".pub");
        InputStream keyStream;
        if (keyCacheFile.isFile()) {
            logger.debug("Fetching key from cache: " + keyCacheFile.getAbsolutePath());
            keyStream = new FileInputStream(keyCacheFile);
        } else {
            URL url = new URL(MessageFormat.format(keyServerUrl, hexKey));
            logger.debug("Fetching key from url: " + url);
            InputStream urlStream = openStream(url, keyServerRetry);
            FileUtils.copyInputStreamToFile(urlStream, keyCacheFile);
            keyStream = new FileInputStream(keyCacheFile);
        }
        try {
            PGPPublicKeyRingCollection publicKeyRing = new BcPGPPublicKeyRingCollection(
                    PGPUtil.getDecoderStream(keyStream));
            PGPPublicKeyRing publicKey = publicKeyRing.getPublicKeyRing(keyID);
            if (publicKey != null) {
                return publicKey;
            }
            return null;
        } finally {
            keyStream.close();
        }
    }

    /**
     * Resolves the signature for the given artifact from the provided session and remote
     * repositories
     * 
     * @param artifact
     * @param session
     * @param repositories
     * @return the resolved signature file
     * @throws ArtifactResolutionException
     *             if no signature can be resolved
     */
    public File getSignatureFile(Artifact artifact, RepositorySystemSession session,
            List<RemoteRepository> repositories) throws ArtifactResolutionException {

        Artifact signatureArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension() + SIGNATURE_EXTENSION, artifact.getVersion());
        ArtifactRequest artifactRequest = new ArtifactRequest(signatureArtifact, repositories, null);
        ArtifactResult dependencyResult = repositorySystem.resolveArtifact(session, artifactRequest);
        Artifact a = dependencyResult.getArtifact();
        if (a != null && a.getFile() != null) {
            return a.getFile();
        }
        //should actually never happen, but better be safe than sorry...
        throw new ArtifactResolutionException(List.of(dependencyResult));

    }

    private InputStream openStream(URL url, int retry) throws IOException {
        while (retry > 0) {
            retry--;
            URLConnection connection = url.openConnection();
            connection.connect();
            if (connection instanceof HttpURLConnection http) {
                int code = http.getResponseCode();
                if (code == HttpURLConnection.HTTP_UNAVAILABLE || code == HttpURLConnection.HTTP_CLIENT_TIMEOUT
                        || code == HttpURLConnection.HTTP_BAD_GATEWAY) {
                    String field = http.getHeaderField("Retry-After");
                    http.disconnect();
                    long wait;
                    if (field == null || !field.isBlank() || !Character.isDigit(field.charAt(0))) {
                        wait = 10;
                    } else {
                        wait = Integer.parseInt(field);
                    }
                    try {
                        TimeUnit.SECONDS.sleep(wait);
                        logger.debug("Server is temporary unavailable [code=" + code + "], waiting " + wait
                                + " seconds before retry, " + retry + " retries left...");
                        continue;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                }
            }
            return connection.getInputStream();
        }
        throw new IOException("retry count exceeded");
    }

}
