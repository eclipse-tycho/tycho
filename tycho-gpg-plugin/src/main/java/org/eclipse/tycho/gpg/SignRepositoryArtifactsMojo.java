/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.gpg;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.gpg.AbstractGpgMojoExtension;
import org.apache.maven.plugins.gpg.ProxySignerWithPublicKeyAccess;
import org.apache.maven.project.MavenProject;
import org.bouncycastle.openpgp.PGPSignature;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.xz.XZArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPSignatureVerifier;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;

/**
 * Modifies the p2 metadata ({@code artifacts.xml}) to add a PGP signature to each included
 * artifact. A signature is added as a {@code pgp.signatures} property on the artifact metadata, in
 * armored form; and the public key of the signer is {@link #addPublicKeyToRepo optionally added} as
 * a {@code pgp.publicKeys} property on the repository metadata, in armored form, and/or
 * {@link #addPublicKeyToRepo optionally added} as a {@code pgp.publicKeys} property on the artifact
 * metadata, in armored form.
 * 
 * @see <a href=
 *      "https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_pgp.html">Using
 *      PGP signatures in p2</a>
 */
@Mojo(name = "sign-p2-artifacts", requiresProject = true, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class SignRepositoryArtifactsMojo extends AbstractGpgMojoExtension {
    enum PGPKeyBehavior {
        skip, replace, merge
    }

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The repository location.
     */
    @Parameter(defaultValue = "${project.build.directory}/repository")
    private File repository;

    /**
     * Configure to {@code true} to generate PGP signature only for artifacts that are
     * <strong>not</strong> signed by a jarsigner.
     */
    @Parameter(defaultValue = "true")
    private boolean skipIfJarsigned;

    /**
     * Configure to {@code true} to generate a PGP signature only for artifacts that do
     * <strong>not</strong> contain a signature that's anchored in Java's trust store, i.e.,
     * anchored in the JDK's cacerts. A JCA certificate, for example, is never anchored.
     */
    @Parameter(defaultValue = "true")
    private boolean skipIfJarsignedAndAnchored;

    /**
     * Configure to {@code true} to generate a PGP signature for binary artifacts.
     */
    @Parameter(defaultValue = "true")
    boolean skipBinaries;

    /**
     * Configure to {@code true} to add the public key of each signature to the repository's
     * metadata.
     */
    @Parameter(defaultValue = "true")
    private boolean addPublicKeyToRepo;

    /**
     * Configure to {@code true} to add the public key of the signature to each signed artifact's
     * metadata.
     */
    @Parameter(defaultValue = "true", alias = "addPublicKeysToArtifacts")
    private boolean addPublicKeyToArtifacts;

    /**
     * Configures how to generate PGP signatures for artifacts that already have one or more PGP
     * signatures, {@code skip} to generate no new PGP signature, {@code replace} to replace the
     * existing signature(s) with a new signature, and {@code merge} to add a new signature to any
     * existing signature(s).
     */
    @Parameter(defaultValue = "skip")
    private PGPKeyBehavior pgpKeyBehavior;

    /**
     * Configure the signer used for PGP signing. Currently supported are {@code gpg} for launching
     * the native {@code gpg} executable, and {@code bc} for using Bouncy Castle libraries. The
     * latter is much faster and it can sign in parallel, so is very much faster.
     */
    @Parameter(property = "tycho.pgp.signer", defaultValue = "gpg")
    private String signer;

    /**
     * Configure the Bouncy Castle {@link #signer} to load the secret keys, stored in armored from,
     * from the specified file. This avoids needing to import the keys into GnuPG's keybox.
     */
    @Parameter(property = "tycho.pgp.signer.bc.secretKeys")
    private File secretKeys;

    /**
     * Configured to specify artifacts that should be signed independently of other settings, e.g.,
     * {@link #skipIfJarsigned}, {@link #skipIfJarsignedAndAnchored}, and {@link #skipBinaries}.
     */
    @Parameter
    private List<String> forceSignature;

    /**
     * Timestamp for reproducible output archive entries, either formatted as ISO 8601 extended
     * offset date-time (e.g. in UTC such as '2011-12-03T10:15:30Z' or with an offset
     * '2019-10-05T20:37:42+06:00'), or as an int representing seconds since the epoch (like
     * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    private String outputTimestamp;

    @Component(role = UnArchiver.class, hint = "zip")
    private ZipUnArchiver zipUnArchiver;

    @Component(role = Archiver.class, hint = "xz")
    private XZArchiver xzArchiver;

    @Component
    private SignedContentFactory signedContentFactory;

    @Component
    private P2RepositoryManager repositoryManager;

    @Override
    protected String getSigner() {
        return signer;
    }

    @Override
    protected File getPGPInfo() {
        var pgpInfo = System.getProperty("org.eclipse.tycho.test.pgp.info");
        return pgpInfo == null ? null : new File(pgpInfo);
    }

    @Override
    protected File getSecretKeys() {
        return secretKeys;
    }

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {

        var signer = newSigner(project);
        var keys = KeyStore.create();

        try {
            var artifactRepository = (IFileArtifactRepository) repositoryManager
                    .getArtifactRepository(new MavenRepositoryLocation("", repository.toURI()));

            var compressed = "true".equals(artifactRepository.getProperty(IRepository.PROP_COMPRESSED));

            if (addPublicKeyToRepo && this.pgpKeyBehavior != PGPKeyBehavior.replace) {
                var repositoryKeys = artifactRepository.getProperty(PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME);
                if (repositoryKeys != null) {
                    keys.add(repositoryKeys);
                }
            }

            var artifactKeys = artifactRepository.query(ArtifactKeyQuery.ALL_KEYS, null);
            var descriptors = artifactKeys.stream().map(artifactRepository::getArtifactDescriptors)
                    .flatMap(Arrays::stream).toList();
            descriptors.parallelStream()
                    .forEach(it -> handle(it, artifactRepository.getArtifactFile(it), signer, keys));

            if (addPublicKeyToRepo && !keys.isEmpty()) {
                artifactRepository.setProperty(PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME,
                        keys.toArmoredString());
            }

            ((SimpleArtifactRepository) artifactRepository).save();

            var artifactsXml = new File(repository, "artifacts.xml");
            var artifactsXmlXz = new File(repository, "artifacts.xml.xz");
            var artifactsJar = new File(repository, "artifacts.jar");
            if (!artifactsXml.exists()) {
                if (artifactsJar.exists()) {
                    zipUnArchiver.setSourceFile(artifactsJar);
                    zipUnArchiver.setDestDirectory(repository);
                    zipUnArchiver.extract();
                }
            }

            // configure for Reproducible Builds based on outputTimestamp value
            MavenArchiver.parseBuildOutputTimestamp(outputTimestamp).map(FileTime::from)
                    .ifPresent(modifiedTime -> xzArchiver.configureReproducibleBuild(modifiedTime));

            xzArchiver.setDestFile(artifactsXmlXz);
            xzArchiver.addFile(artifactsXml, artifactsXml.getName());
            xzArchiver.createArchive();
            if (compressed) {
                artifactsXml.delete();
            }

        } catch (RuntimeException e) {
            var cause = e.getCause();
            if (cause instanceof MojoExecutionException) {
                throw (MojoExecutionException) cause;
            }
            if (cause instanceof MojoFailureException) {
                throw (MojoFailureException) cause;
            }
            throw e;
        } catch (ProvisionException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void handle(IArtifactDescriptor artifactDescriptor, File artifact, ProxySignerWithPublicKeyAccess signer,
            KeyStore allKeys) {
        if (artifact != null) {
            var existingKeys = artifactDescriptor.getProperty(PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME);
            var existingSignatures = artifactDescriptor.getProperty(PGPSignatureVerifier.PGP_SIGNATURES_PROPERTY_NAME);

            if (existingSignatures != null && pgpKeyBehavior == PGPKeyBehavior.skip) {
                return;
            }

            IArtifactKey artifactKey = artifactDescriptor.getArtifactKey();

            if (forceSignature == null || !forceSignature.contains(artifactKey.getId())) {
                var classifier = artifactKey.getClassifier();
                var isBinary = "binary".equals(classifier);
                if (skipBinaries && isBinary) {
                    return;
                }

                if (!isBinary) {
                    try {
                        var signedContent = signedContentFactory.getSignedContent(artifact);
                        if (signedContent.isSigned()) {
                            for (var signerInfo : signedContent.getSignerInfos()) {
                                // Check that the signature was produced within the validity range of the certificate.
                                // If invalid, this throws CertificateExpiredException or CertificateNotYetValidException.
                                // That ensures we continue the logic that follows as if the content were not signed.
                                signedContent.checkValidity(signerInfo);
                            }

                            if (skipIfJarsigned) {
                                return;
                            }
                            if (skipIfJarsignedAndAnchored) {
                                for (var signerInfo : signedContent.getSignerInfos()) {
                                    if (signerInfo.getTrustAnchor() != null) {
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        //$FALL-THROUGH$ Treat as unsigned.
                    }
                }
            }

            if (pgpKeyBehavior == PGPKeyBehavior.replace) {
                existingSignatures = null;
                existingKeys = null;
            }

            try {
                var signatures = signer.generateSignature(artifact);
                var signerKeys = signatures.all().stream().map(PGPSignature::getKeyID)
                        .flatMap(id -> signer.getPublicKeys().getKeys(id).stream()).toList();
                var keyStore = KeyStore.create(existingKeys);
                keyStore.add(signerKeys);
                allKeys.add(keyStore);

                signatures.add(existingSignatures);

                ((ArtifactDescriptor) artifactDescriptor).setProperty(PGPSignatureVerifier.PGP_SIGNATURES_PROPERTY_NAME,
                        signatures.toArmoredString());

                if (addPublicKeyToArtifacts) {
                    ((ArtifactDescriptor) artifactDescriptor).setProperty(
                            PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME, keyStore.toArmoredString());
                }
            } catch (MojoExecutionException | IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}
