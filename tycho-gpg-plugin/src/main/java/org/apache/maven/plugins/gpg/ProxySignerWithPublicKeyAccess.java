/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.apache.maven.plugins.gpg;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.bouncycastle.openpgp.PGPException;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;
import org.codehaus.plexus.util.cli.Commandline;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.tycho.gpg.BouncyCastleSigner;
import org.eclipse.tycho.gpg.KeyStore;
import org.eclipse.tycho.gpg.SignatureStore;

public class ProxySignerWithPublicKeyAccess extends AbstractGpgSigner {

    private final AbstractGpgSigner delegate;

    private final BouncyCastleSigner signer;

    private KeyStore publicKeys;

    public ProxySignerWithPublicKeyAccess(AbstractGpgSigner delegate, String signer, File pgpInfo, File secretKeys) {
        this.delegate = delegate;
        this.setLog(delegate.getLog());
        // The pgpInfo is used only for testing purposes.
        if (BouncyCastleSigner.NAME.equals(signer) || pgpInfo != null || secretKeys != null) {
            try {
                this.signer = getSigner(pgpInfo, secretKeys);
            } catch (MojoExecutionException | MojoFailureException | IOException | PGPException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.signer = null;
        }
    }

    public KeyStore getPublicKeys() {
        if (publicKeys == null) {
            try {
                publicKeys = KeyStore.create(getKeys(true));
            } catch (MojoExecutionException e) {
                new RuntimeException(e.getMessage(), e);
            }
        }
        return publicKeys;
    }

    protected BouncyCastleSigner getSigner(File pgpInfo, File secretKeys)
            throws MojoExecutionException, IOException, MojoFailureException, PGPException {
        keyname = delegate.keyname;
        var signer = new BouncyCastleSigner();
        signer.setLog(getLog());
        if (pgpInfo != null) {
            signer.configureFromPGPInfo(keyname, pgpInfo);
            publicKeys = KeyStore.create(signer.getPublicKeys());
        } else if (secretKeys != null) {
            signer.configure(keyname, delegate.passphrase, null,
                    Files.readString(secretKeys.toPath(), StandardCharsets.US_ASCII));
            publicKeys = KeyStore.create(signer.getPublicKeys());
        } else {
            var publicKeys = getPublicKeys().toArmoredString();
            var gpgSecretKeys = getKeys(false);
            if (keyname == null) {
                // Determine which key is used for signing by signing a file.
                var dummy = Files.createTempFile("dummy", ".txt");
                var signature = Files.createTempFile("dummy", ".asc");
                Files.delete(signature);
                delegate.generateSignatureForFile(dummy.toFile(), signature.toFile());
                var signatures = SignatureStore.create(Files.readString(signature, StandardCharsets.US_ASCII));
                keyname = PGPPublicKeyService.toHex(signatures.all().iterator().next().getKeyID());
                Files.delete(dummy);
                Files.delete(signature);
            }
            signer.configure(keyname, delegate.passphrase, publicKeys, gpgSecretKeys);
        }
        return signer;
    }

    public SignatureStore generateSignature(File file) throws MojoExecutionException {
        try {
            if (signer != null) {
                return signer.generateSignature(file);
            } else {
                File signatureFile;
                synchronized (delegate) {
                    // gpg generally doesn't like to sign in parallel.
                    signatureFile = delegate.generateSignatureForArtifact(file);
                }
                var signatureStore = SignatureStore
                        .create(Files.readString(signatureFile.toPath(), StandardCharsets.US_ASCII));
                signatureFile.delete();
                return signatureStore;
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    protected void generateSignatureForFile(File file, File signature) throws MojoExecutionException {
        if (signer != null) {
            try {
                Files.writeString(signature.toPath(), signer.generateSignature(file).toArmoredString());
            } catch (IOException | PGPException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } else {
            delegate.generateSignatureForFile(file, signature);
        }
    }

    /**
     * Fetches the public or secrete keys using gpg.
     */
    private String getKeys(boolean isPublic) throws MojoExecutionException {
        var cmd = new Commandline();

        var executable = "gpg" + (Os.isFamily(Os.FAMILY_WINDOWS) ? ".exe" : "");
        cmd.setExecutable(executable);

        if (delegate.args != null) {
            for (var arg : delegate.args) {
                cmd.createArg().setValue(arg);
            }
        }

        if (delegate.homeDir != null) {
            cmd.createArg().setValue("--homedir");
            cmd.createArg().setFile(delegate.homeDir);
        }

        InputStream in = null;
        if (isPublic) {
            cmd.createArg().setValue("--export");
        } else {
            cmd.createArg().setValue("--export-secret-keys");
            if (delegate.passphrase != null) {
                var versionParser = GpgVersionParser.parse(executable);
                var gpgVersion = versionParser.getGpgVersion();
                if (gpgVersion.isAtLeast(GpgVersion.parse("2.0"))) {
                    // required for option --passphrase-fd since GPG 2.0
                    cmd.createArg().setValue("--batch");
                }

                if (gpgVersion.isAtLeast(GpgVersion.parse("2.1"))) {
                    // required for option --passphrase-fd since GPG 2.1
                    cmd.createArg().setValue("--pinentry-mode");
                    cmd.createArg().setValue("loopback");
                }

                // make --passphrase-fd effective in gpg2
                cmd.createArg().setValue("--passphrase-fd");
                cmd.createArg().setValue("0");

                // Prepare the input stream which will be used to pass the passphrase to the executable
                in = new ByteArrayInputStream(delegate.passphrase.getBytes());

                if (StringUtils.isNotEmpty(delegate.secretKeyring)) {
                    if (gpgVersion.isBefore(GpgVersion.parse("2.1"))) {
                        cmd.createArg().setValue("--secret-keyring");
                        cmd.createArg().setValue(delegate.secretKeyring);
                    } else {
                        getLog().warn("'secretKeyring' is an obsolete option and is ignored. All secret keys "
                                + "are stored in the 'private-keys-v1.d' directory below the GnuPG home directory");
                    }
                }
            }
        }

        cmd.createArg().setValue("--armor");

        if (!delegate.defaultKeyring) {
            cmd.createArg().setValue("--no-default-keyring");
        }

        if (StringUtils.isNotEmpty(delegate.publicKeyring)) {
            cmd.createArg().setValue("--keyring");
            cmd.createArg().setValue(delegate.publicKeyring);
        }

        if (delegate.keyname != null) {
            cmd.createArg().setValue(delegate.keyname);
        }

        // ----------------------------------------------------------------------------
        // Execute the command line
        // ----------------------------------------------------------------------------

        try {
            var systemOut = new StringStreamConsumer();
            var exitCode = CommandLineUtils.executeCommandLine(cmd, in, systemOut, systemOut);
            if (exitCode != 0) {
                throw new MojoExecutionException("Exit code: " + exitCode);
            }
            return systemOut.getOutput();
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to execute gpg command", e);
        }
    }

    @Override
    public String signerName() {
        return signer.signerName();
    }

    @Override
    public String getKeyInfo() {
        return signer.getKeyInfo();
    }
}
