/**
 * Copyright (c) 2022 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.tycho.gpg;

import static org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService.toHex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.gpg.AbstractGpgSigner;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;

public class BouncyCastleSigner extends AbstractGpgSigner {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private String publicKeys;

    private String secretKeys;

    private PGPSecretKey secretKey;

    private PGPPrivateKey privateKey;

    public BouncyCastleSigner(String keyname, String passphrase, String publicKeys, String secretKeys)
            throws IOException, PGPException {
        this.passphrase = passphrase;
        this.publicKeys = publicKeys;
        this.secretKeys = secretKeys;
        this.keyname = keyname.toLowerCase();
        initPrivateKey();
    }

    /**
     * This loads the key ring as saved by {@link #dump(Path)}.
     * 
     * @throws IOException
     * @throws PGPException
     * 
     * @see #main(String[])
     */
    public BouncyCastleSigner(String keyname, File pgpInfo) throws PGPException, IOException {
        this.keyname = keyname;
        init(pgpInfo);
    }

    /**
     * This creates a new key ring for this user ID with this passphrase.
     * 
     * @throws IOException
     * @throws PGPException
     */
    public BouncyCastleSigner(String passphrase, String... userIDs) throws PGPException, IOException {
        this.passphrase = passphrase;
        init(userIDs);
    }

    public String getPublicKeys() {
        return publicKeys;
    }

    public SignatureStore generateSignature(File file) throws PGPException, IOException {
        var publicKey = secretKey.getPublicKey();
        var signatureGenerator = new PGPSignatureGenerator(
                new JcaPGPContentSignerBuilder(publicKey.getAlgorithm(), HashAlgorithmTags.SHA256)
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME));
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
        var subpackets = new PGPSignatureSubpacketGenerator();
        subpackets.setIssuerFingerprint(false, publicKey);
        var userIDs = publicKey.getUserIDs();
        if (userIDs.hasNext()) {
            subpackets.addSignerUserID(false, userIDs.next());
        }
        signatureGenerator.setHashedSubpackets(subpackets.generate());
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
        signatureGenerator.update(Files.readAllBytes(file.toPath()));

        var signatureStore = SignatureStore.create(signatureGenerator.generate());
        return signatureStore;
    }

    @Override
    protected void generateSignatureForFile(File file, File signature) throws MojoExecutionException {
        try {
            Files.writeString(signature.toPath(), generateSignature(file).toArmoredString());
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Initializes by loading a key ring.
     */
    private void init(File pgpInfo) throws PGPException, IOException {
        var lines = Files.readAllLines(pgpInfo.toPath(), StandardCharsets.US_ASCII);
        passphrase = lines.get(0);
        var index = lines.indexOf("-----END PGP PUBLIC KEY BLOCK-----");
        publicKeys = String.join("\n", lines.subList(1, index + 1));
        secretKeys = String.join("\n", lines.subList(index + 1, lines.size()));
        initPrivateKey();
    }

    private void initPrivateKey() throws IOException, PGPException {
        try (var stream = PGPUtil
                .getDecoderStream(new ByteArrayInputStream(secretKeys.getBytes(StandardCharsets.US_ASCII)))) {
            for (var object : new JcaPGPObjectFactory(stream)) {
                if (object instanceof PGPSecretKeyRing) {
                    secretKey = getSecretKey(((PGPSecretKeyRing) object));
                    if (secretKey != null) {
                        privateKey = getPrivateKey(secretKey);
                        return;
                    }
                }
            }
        }
        throw new PGPException("A key for keyname '" + keyname + "' not found.");
    }

    /**
     * Initializes by generating a key ring for the given user ID.
     */
    private void init(String... userIDs) throws PGPException, IOException {
        var keyPairGenerator = new RSAKeyPairGenerator();
        keyPairGenerator
                .init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), new SecureRandom(), 4096, 12));

        var now = new Date(0);
        var publicKeyRings = new ArrayList<PGPPublicKeyRing>();
        var secretKeyRings = new ArrayList<PGPSecretKeyRing>();
        for (var userID : userIDs) {
            var signingKeyPair = new BcPGPKeyPair(PGPPublicKey.RSA_SIGN, keyPairGenerator.generateKeyPair(), now);
            var signatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
            signatureSubpacketGenerator.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER);
            signatureSubpacketGenerator.setPreferredSymmetricAlgorithms(false,
                    new int[] { SymmetricKeyAlgorithmTags.AES_256, SymmetricKeyAlgorithmTags.AES_128 });
            signatureSubpacketGenerator.setPreferredHashAlgorithms(false,
                    new int[] { HashAlgorithmTags.SHA512, HashAlgorithmTags.SHA256 });
            signatureSubpacketGenerator.setPreferredCompressionAlgorithms(false,
                    new int[] { CompressionAlgorithmTags.ZIP, CompressionAlgorithmTags.BZIP2 });

            var encryptionKeyPair = new BcPGPKeyPair(PGPPublicKey.RSA_ENCRYPT, keyPairGenerator.generateKeyPair(), now);
            var encryptionSubpacketGenerator = new PGPSignatureSubpacketGenerator();
            encryptionSubpacketGenerator.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);

            var digestCalculator = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
            var signatureSubpacketVector = signatureSubpacketGenerator.generate();
            var contentSignerBuilder = new BcPGPContentSignerBuilder(PGPPublicKey.RSA_SIGN, HashAlgorithmTags.SHA256);
            var secretKeyEncryptorBuilder = new BcPBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256);
            var keyRingGenerator = new PGPKeyRingGenerator(PGPPublicKey.RSA_SIGN, signingKeyPair, userID,
                    digestCalculator, signatureSubpacketVector, null, contentSignerBuilder,
                    secretKeyEncryptorBuilder.build(passphrase.toCharArray()));
            keyRingGenerator.addSubKey(encryptionKeyPair, encryptionSubpacketGenerator.generate(), null);
            publicKeyRings.add(keyRingGenerator.generatePublicKeyRing());
            secretKeyRings.add(keyRingGenerator.generateSecretKeyRing());
        }

        var publicKeyOut = new ByteArrayOutputStream();
        try (var targetStream = new ArmoredOutputStream(publicKeyOut)) {
            targetStream.setHeader("Version", null);
            publicKeyRings.stream().map(it -> toHex(it.getPublicKey().getFingerprint()))
                    .forEach(it -> targetStream.addHeader("Key", it));
            new PGPPublicKeyRingCollection(publicKeyRings).encode(targetStream);
        }

        var secretKeyOut = new ByteArrayOutputStream();
        try (var targetStream = new ArmoredOutputStream(secretKeyOut)) {
            targetStream.setHeader("Version", null);
            new PGPSecretKeyRingCollection(secretKeyRings).encode(targetStream);
        }

        publicKeys = publicKeyOut.toString(StandardCharsets.US_ASCII);
        secretKeys = secretKeyOut.toString(StandardCharsets.US_ASCII);

        secretKey = getSecretKey(secretKeyRings.get(0));
        privateKey = getPrivateKey(this.secretKey);
    }

    private PGPSecretKey getSecretKey(PGPSecretKeyRing secretKeyRing) throws PGPException {
        for (var secretKeys = secretKeyRing.getSecretKeys(); secretKeys.hasNext();) {
            var pgpSecretKey = secretKeys.next();
            if (keyname == null) {
                return pgpSecretKey;
            }
            var fingerprint = toHex(pgpSecretKey.getPublicKey().getFingerprint());
            if (fingerprint.endsWith(keyname)) {
                return pgpSecretKey;
            }
        }
        return null;
    }

    private PGPPrivateKey getPrivateKey(PGPSecretKey pgpSecretKey) throws PGPException {
        var pbeSecretKeyDecryptor = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
                .build(passphrase.toCharArray());
        return pgpSecretKey.extractPrivateKey(pbeSecretKeyDecryptor);
    }

    public void dump(Path target) throws IOException {
        try (var out = new PrintStream(Files.newOutputStream(target))) {
            out.println(passphrase);
            out.print(publicKeys);
            out.print(secretKeys);
        }
    }

    public static void main(String[] args) throws Exception {
        // Creates a file with multiple public and private keys that can be used for testing.
        var target = Files.createTempFile("pgp", ".info");
        System.out.println(target);
        new BouncyCastleSigner("passphrase", "Tester1 <tester1@example.com>", "Tester2 <tester2@example.com>")
                .dump(target);
        new BouncyCastleSigner(null, target.toFile());
        Files.copy(target, System.out);
    }
}
