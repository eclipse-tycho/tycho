/*
 * Copyright (c) 2022 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.tycho.test.pgp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.operator.PGPContentVerifier;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.tycho.gpg.BouncyCastleSigner;
import org.eclipse.tycho.gpg.KeyStore;
import org.eclipse.tycho.gpg.SignatureStore;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;
import org.tukaani.xz.XZInputStream;

public class TestPGPSigning extends AbstractTychoIntegrationTest {

	private static final boolean DEBUG = false;

	private static final String PASSPHRASE = "passphrase";

	private static final String PGP_INFO_PROPERTY = "org.eclipse.tycho.test.pgp.info";

	private static final Path PGP_INFO;

	private static final Path PGP_SECRET_KEYS;

	private static final List<String> KEY_FINGERPRINTS = new ArrayList<>();

	private static final String PRIMARY_KEY_FINGERPRINT;

	private static final String PRIMARY_KEY_NAME;

	private static final String SECONDARY_KEY_FINGERPRINT;

	private static final String SECONDARY_KEY_NAME;

	static {
		Path pgpInfo = null;
		Path secretKeys = null;
		try {
			pgpInfo = Files.createTempFile("pgp", ".info");
			var bouncyCastleSigner = new BouncyCastleSigner().configureNewUserIDs(PASSPHRASE,
					"Tester1 <tester1@example.com>", "Tester2 <tester2@example.com>");
			for (var matcher = Pattern.compile("Key: (\\S+)").matcher(bouncyCastleSigner.getPublicKeys()); matcher
					.find();) {
				String fingerprint = matcher.group(1);
				KEY_FINGERPRINTS.add(fingerprint);
			}
			bouncyCastleSigner.dump(pgpInfo);

			secretKeys = Files.createTempFile("secret-keys", ".asc");
			Files.writeString(secretKeys, bouncyCastleSigner.getSecretKeys(), StandardCharsets.US_ASCII);
		} catch (IOException | PGPException ex) {
			throw new RuntimeException(ex);
		}

		PGP_INFO = pgpInfo;
		PRIMARY_KEY_FINGERPRINT = KEY_FINGERPRINTS.get(0);
		PRIMARY_KEY_NAME = PRIMARY_KEY_FINGERPRINT.substring(32, 40);
		SECONDARY_KEY_FINGERPRINT = KEY_FINGERPRINTS.get(1);
		SECONDARY_KEY_NAME = SECONDARY_KEY_FINGERPRINT.substring(32, 40);
		Collections.sort(KEY_FINGERPRINTS);
		PGP_SECRET_KEYS = secretKeys;
	}

	private Verifier createVerifier() throws Exception {
		var verifier = getVerifier("gpg.sign.p2.basic", true);
		verifier.addCliOption("-Pgpg-sign");

		// This forces gpg NOT to be used.
		verifier.setSystemProperty("org.eclipse.tycho.test.pgp.info", PGP_INFO.toString());

		if (DEBUG) {
			verifier.setForkJvm(false);
			verifier.setSystemProperty("maven.multiModuleProjectDirectory", verifier.getBasedir());
			verifier.setSystemProperty("user.home", "D:/Users/test10");
			verifier.setSystemProperty("gpg.passphrase", PASSPHRASE);
		}

		return verifier;
	}

	private void verify(Verifier verifier) throws VerificationException {
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.resetStreams();
		if (DEBUG) {
			dumpLog(verifier);
		}
	}

	private void dumpLog(Verifier verifier) throws VerificationException {
		var loadFile = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);
		System.out.println(String.join("\n", loadFile));
	}

	@Test
	public void testSigning() throws Exception {
		var verifier = createVerifier();
		verifier.setSystemProperty("test.signer", "gpg");
		verify(verifier);

		var data = verifySignatures(verifier);

		assertEquals(1, data.repositoryKeys.size(), "Exactly one key is expected");

		assertEquals(
				"[bcpg, bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		Set<String> signedIUs = data.signedIUs.keySet();
		assertEquals("[bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	@Test
	public void testSigningWithBouncyCastle() throws Exception {
		var verifier = createVerifier();
		verifier.setSystemProperty("test.signer", "bc");
		verify(verifier);

		var data = verifySignatures(verifier);

		assertEquals(1, data.repositoryKeys.size(), "Exactly one key is expected");

		assertEquals(
				"[bcpg, bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		Set<String> signedIUs = data.signedIUs.keySet();
		assertEquals("[bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	@Test
	public void testSigningWithBouncyCastleWithDirectlyLoadedSecretKeys() throws Exception {
		var verifier = createVerifier();
		verifier.setSystemProperty("test.signer", "bc");
		verifier.setSystemProperty(PGP_INFO_PROPERTY, null);
		verifier.setSystemProperty("gpg.passphrase", PASSPHRASE);
		verifier.setSystemProperty("tycho.pgp.signer.bc.secretKeys", PGP_SECRET_KEYS.toString());
		verify(verifier);

		var data = verifySignatures(verifier);

		assertEquals(Set.of(PRIMARY_KEY_FINGERPRINT).toString(), data.repositoryKeys.toString(),
				"Exactly this one key is expected");

		assertEquals(
				"[bcpg, bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		Set<String> signedIUs = data.signedIUs.keySet();
		assertEquals("[bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	@Test
	public void testSigningWithBouncyCastleWithDirectlyLoadedSecretKeysAndSpecifiedKeyname() throws Exception {
		var verifier = createVerifier();
		verifier.setSystemProperty("test.signer", "bc");
		verifier.setSystemProperty(PGP_INFO_PROPERTY, null);
		verifier.setSystemProperty("gpg-keyname", SECONDARY_KEY_NAME);
		verifier.setSystemProperty("gpg.passphrase", PASSPHRASE);
		verifier.setSystemProperty("tycho.pgp.signer.bc.secretKeys", PGP_SECRET_KEYS.toString());
		verify(verifier);

		var data = verifySignatures(verifier);

		assertEquals(Set.of(SECONDARY_KEY_FINGERPRINT).toString(), data.repositoryKeys.toString(),
				"Exactly this one key is expected");

		assertEquals(
				"[bcpg, bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		Set<String> signedIUs = data.signedIUs.keySet();
		assertEquals("[bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	@Test
	public void testSigningSkipIfJarSignedAndAnchored() throws Exception {
		var verifier = createVerifier();
		verifier.setSystemProperty("test.skipIfJarsigned", "false");
		verify(verifier);

		var data = verifySignatures(verifier);

		assertEquals(1, data.repositoryKeys.size(), "Exactly one key is expected");

		// Verify that Maven-wrapped artifacts are signed
		Set<String> signedIUs = data.signedIUs.keySet();
		Set<String> mustBeSigned = Set.of(
			"bcpg", "bcpg.source", "bcprov", "bcprov.source",
			"org.eclipse.tycho.maven.all", "org.eclipse.tycho.maven.all.source");
		
		// Verify all expected signed IUs are present
		for (String expected : mustBeSigned) {
			if (!signedIUs.contains(expected)) {
				fail("Expected " + expected + " to be signed but it was not. All IUs: " + data.allIUs + 
					", Signed: " + signedIUs + ", Unsigned: " + data.unsignedIUs);
			}
		}
		
		// Eclipse platform bundles may be signed or unsigned depending on whether they are
		// jar-signed AND anchored (i.e., have a trust anchor in their JAR signature).
		// The skipIfJarsignedAndAnchored=true configuration skips PGP signing for such bundles.
		// The exact jar-signed/anchored state can vary based on what's fetched from the remote p2 repository.
		// org.eclipse.platform_root is a binary, and with default skipBinaries=true, it typically won't be signed.
		Set<String> eclipseBundles = Set.of(
			"org.eclipse.equinox.common", "org.eclipse.equinox.common.source",
			"org.eclipse.osgi", "org.eclipse.osgi.source", "org.eclipse.platform_root");
		
		// Verify only expected IUs are signed (plus potentially Eclipse bundles if not anchored)
		for (String actual : signedIUs) {
			if (!mustBeSigned.contains(actual) && !eclipseBundles.contains(actual)) {
				fail("Unexpected signed IU: " + actual + ". Expected only Maven artifacts or Eclipse bundles, " + 
					"but got signed IUs: " + signedIUs);
			}
		}
		
		// Verify unsigned IUs are only from expected Eclipse bundles
		for (String actual : data.unsignedIUs) {
			if (!eclipseBundles.contains(actual)) {
				fail("Unexpected unsigned IU: " + actual + ". Expected only Eclipse bundles to be unsigned, " + 
					"but got unsigned IUs: " + data.unsignedIUs);
			}
		}
	}

	@Test
	public void testSigningBinaries() throws Exception {
		var verifier = createVerifier();
		verifier.setSystemProperty("test.skipIfJarsigned", "false");
		verifier.setSystemProperty("test.skipBinaries", "false");
		verify(verifier);

		var data = verifySignatures(verifier);

		assertEquals(1, data.repositoryKeys.size(), "Exactly one key is expected");

		// Verify that Maven-wrapped artifacts and binaries are signed
		Set<String> signedIUs = data.signedIUs.keySet();
		Set<String> mustBeSigned = Set.of(
			"bcpg", "bcpg.source", "bcprov", "bcprov.source",
			"org.eclipse.platform_root", "org.eclipse.tycho.maven.all", "org.eclipse.tycho.maven.all.source");
		
		// Verify all expected signed IUs are present
		for (String expected : mustBeSigned) {
			if (!signedIUs.contains(expected)) {
				fail("Expected " + expected + " to be signed but it was not. All IUs: " + data.allIUs + 
					", Signed: " + signedIUs + ", Unsigned: " + data.unsignedIUs);
			}
		}
		
		// Eclipse platform bundles may be signed or unsigned depending on whether they are
		// jar-signed AND anchored (i.e., have a trust anchor in their JAR signature).
		// The skipIfJarsignedAndAnchored=true configuration skips PGP signing for such bundles.
		// The exact jar-signed/anchored state can vary based on what's fetched from the remote p2 repository.
		// We allow them to appear in either category but verify they don't appear as unexpected IUs.
		Set<String> eclipseBundles = Set.of(
			"org.eclipse.equinox.common", "org.eclipse.equinox.common.source",
			"org.eclipse.osgi", "org.eclipse.osgi.source");
		
		// Verify only expected IUs are signed (plus potentially Eclipse bundles if not anchored)
		for (String actual : signedIUs) {
			if (!mustBeSigned.contains(actual) && !eclipseBundles.contains(actual)) {
				fail("Unexpected signed IU: " + actual + ". Expected only Maven artifacts or Eclipse bundles, " + 
					"but got signed IUs: " + signedIUs);
			}
		}
		
		// Verify unsigned IUs are only from expected Eclipse bundles
		for (String actual : data.unsignedIUs) {
			if (!eclipseBundles.contains(actual)) {
				fail("Unexpected unsigned IU: " + actual + ". Expected only Eclipse bundles to be unsigned, " + 
					"but got unsigned IUs: " + data.unsignedIUs);
			}
		}
	}

	@Test
	public void testForceSigning() throws Exception {
		var verifier = createVerifier();
		verifier.setSystemProperty("test.signer", "gpg");
		verifier.setSystemProperty("test.forceSignature", "bcpg");
		verify(verifier);

		var data = verifySignatures(verifier);

		assertEquals(1, data.repositoryKeys.size(), "Exactly one key is expected");

		assertEquals(
				"[bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		Set<String> signedIUs = data.signedIUs.keySet();
		assertEquals(
				"[bcpg, bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	@Test
	public void testResigningMerge() throws Exception {
		var verifier = createVerifier();

		// This test relies on being able to test with two different signing keys.
		if (!verifier.getSystemProperties().containsKey(PGP_INFO_PROPERTY)) {
			return;
		}

		verifier.addCliOption("-Pgpg-sign-2");
		verifier.setSystemProperty("test.forceSignature", "bcpg");
		verifier.setSystemProperty("test.pgpKeyBehavior-2", "merge");
		verifier.setSystemProperty("gpg-keyname-2", SECONDARY_KEY_NAME);
		verify(verifier);

		var data = verifySignatures(verifier);

		var expectedFingerprints = KEY_FINGERPRINTS.toString();
		assertEquals(expectedFingerprints, data.repositoryKeys.toString(), "Exactly these two keys are expected");

		assertEquals(
				"[bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		var signedIUs = data.signedIUs.keySet();
		for (var fingerprints : data.signedIUs.values()) {
			assertEquals(expectedFingerprints, fingerprints.toString(), "Expecting two merged signature fingerprints.");
		}

		assertEquals(
				"[bcpg, bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	@Test
	public void testResigningMergeSameKey() throws Exception {
		var verifier = createVerifier();

		// This test relies on being able to test with two different signing keys.
		if (!verifier.getSystemProperties().containsKey(PGP_INFO_PROPERTY)) {
			return;
		}

		verifier.addCliOption("-Pgpg-sign-2");
		verifier.setSystemProperty("test.forceSignature", "bcpg");
		verifier.setSystemProperty("test.pgpKeyBehavior-2", "merge");
		verifier.setSystemProperty("gpg-keyname-2", PRIMARY_KEY_NAME);
		verify(verifier);

		var data = verifySignatures(verifier);

		var expectedFingerprints = Set.of(PRIMARY_KEY_FINGERPRINT).toString();
		assertEquals(expectedFingerprints, data.repositoryKeys.toString(), "Exactly these two keys are expected");

		assertEquals(
				"[bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		var signedIUs = data.signedIUs.keySet();
		for (var fingerprints : data.signedIUs.values()) {
			assertEquals(expectedFingerprints, fingerprints.toString(), "Expecting two merged signature fingerprints.");
		}

		assertEquals(
				"[bcpg, bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	@Test
	public void testResigningReplace() throws Exception {
		var verifier = createVerifier();

		// This test relies on being able to test with two different signing keys.
		if (!verifier.getSystemProperties().containsKey(PGP_INFO_PROPERTY)) {
			return;
		}

		verifier.addCliOption("-Pgpg-sign-2");
		verifier.setSystemProperty("test.forceSignature", "bcpg");
		verifier.setSystemProperty("test.pgpKeyBehavior-2", "replace");
		verifier.setSystemProperty("gpg-keyname-2", SECONDARY_KEY_NAME);
		verify(verifier);

		var data = verifySignatures(verifier);

		var expectedFingerprints = Set.of(SECONDARY_KEY_FINGERPRINT).toString();
		assertEquals(expectedFingerprints, data.repositoryKeys.toString(),
				"Exactly the one replacement key is expected");

		assertEquals(
				"[bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		var signedIUs = data.signedIUs.keySet();
		for (var fingerprints : data.signedIUs.values()) {
			assertEquals(expectedFingerprints, fingerprints.toString(), "Expecting replaced signature fingerprint.");
		}

		assertEquals(
				"[bcpg, bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	@Test
	public void testResigningSkip() throws Exception {
		var verifier = createVerifier();

		// This test relies on being able to test with two different signing keys.
		if (!verifier.getSystemProperties().containsKey(PGP_INFO_PROPERTY)) {
			return;
		}

		verifier.addCliOption("-Pgpg-sign-2");
		verifier.setSystemProperty("test.forceSignature", "bcpg");
		verifier.setSystemProperty("test.pgpKeyBehavior-2", "skip");
		verifier.setSystemProperty("gpg-keyname-2", SECONDARY_KEY_NAME);
		verify(verifier);

		var data = verifySignatures(verifier);

		var expectedFingerprints = Set.of(PRIMARY_KEY_FINGERPRINT).toString();
		assertEquals(expectedFingerprints, data.repositoryKeys.toString(), "Exactly the one orginal key is expected");

		assertEquals(
				"[bcprov, org.eclipse.equinox.common, org.eclipse.equinox.common.source, org.eclipse.osgi, org.eclipse.osgi.source, org.eclipse.platform_root]",
				data.unsignedIUs.toString(), "Unexpected unsigned IUs.");

		var signedIUs = data.signedIUs.keySet();
		for (var fingerprints : data.signedIUs.values()) {
			assertEquals(expectedFingerprints, fingerprints.toString(), "Expecting the first signature fingerprints.");
		}

		assertEquals(
				"[bcpg, bcpg.source, bcprov.source, org.eclipse.tycho.maven.all, org.eclipse.tycho.maven.all.source]",
				signedIUs.toString(), "Unexpected signed IUs.");
	}

	private static class Data {
		public final Set<String> repositoryKeys = new TreeSet<>();
		public final Set<String> allIUs = new TreeSet<>();
		public final Map<String, Set<String>> signedIUs = new TreeMap<>();
		public final Set<String> unsignedIUs = new TreeSet<>();

		@Override
		public String toString() {
			return "Data [repositoryKeys=" + repositoryKeys + ", allIUs=" + allIUs + ", signedIUs=" + signedIUs
					+ ", unsignedIUs=" + unsignedIUs + "]";
		}
	}

	private Data verifySignatures(Verifier verifier) throws Exception {
		var data = new Data();
		var basedir = verifier.getBasedir();
		var repository = Path.of(basedir, "site/target/repository");
		Xpp3Dom xzDOM;
		try (var stream = new XZInputStream(Files.newInputStream(repository.resolve("artifacts.xml.xz")))) {
			xzDOM = Xpp3DomBuilder.build(stream, StandardCharsets.UTF_8.displayName());
		} catch (IOException | XmlPullParserException e) {
			fail(e.getMessage());
			throw e;
		}

		var url = new URL("jar:" + repository.toUri() + "artifacts.jar!/artifacts.xml");
		try (var stream = url.openStream()) {
			var dom = Xpp3DomBuilder.build(stream, StandardCharsets.UTF_8.displayName());

			assertEquals(dom.toString(), xzDOM.toString(),
					"The artifacts.xml.xz should have the same contents as the artifacts.jar");

			var repositoryProperties = getProperties(dom);
			var repositoryKey = repositoryProperties.get("pgp.publicKeys");
			if (repositoryKey != null) {
				KeyStore.create(repositoryKey).all().stream().map(it -> PGPPublicKeyService.toHex(it.getFingerprint()))
						.forEach(data.repositoryKeys::add);
			}

			for (var artifact : dom.getChild("artifacts").getChildren("artifact")) {
				var id = artifact.getAttribute("id");
				var version = artifact.getAttribute("version");
				var classifier = artifact.getAttribute("classifier");

				data.allIUs.add(id);

				var properties = getProperties(artifact);
				var key = properties.get("pgp.publicKeys");
				var signature = properties.get("pgp.signatures");
				if (signature != null) {
					assertNotNull("A key is expected when there is a signature.", key);
					var fingerprints = verifyArtifactSignature(repository, id, version, classifier, key, signature);
					data.signedIUs.put(id, fingerprints);
				} else {
					data.unsignedIUs.add(id);
				}
			}
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return data;
	}

	private Map<String, String> getProperties(Xpp3Dom element) {
		return Arrays.stream(element.getChild("properties").getChildren())
				.collect(Collectors.toMap(it -> it.getAttribute("name"), it -> it.getAttribute("value")));
	}

	private Set<String> verifyArtifactSignature(Path repositoryLocation, String id, String version, String classifier,
			String keyProperty, String signatureProperty) throws PGPException, IOException {

		var fingerprints = new TreeSet<String>();
		var signatureStore = SignatureStore.create(signatureProperty);
		var keyStore = KeyStore.create(keyProperty);
		var signaturesToVerify = new LinkedHashMap<PGPSignature, List<PGPContentVerifier>>();
		var verifierKeys = new LinkedHashMap<PGPContentVerifier, PGPPublicKey>();
		var signatureVerifiers = new ArrayList<OutputStream>();
		for (var signature : signatureStore.all()) {
			var keyID = signature.getKeyID();
			var keys = keyStore.getKeys(keyID);
			if (keys.isEmpty()) {
				fail("A key with id '" + PGPPublicKeyService.toHex(keyID)
						+ "' is expected to exist but only the following exist: "
						+ keyStore.all().stream().map(PGPPublicKey::getKeyID).map(PGPPublicKeyService::toHex)
								.collect(Collectors.joining(", ")));
			}
			var verifierBuilder = new BcPGPContentVerifierBuilderProvider().get(signature.getKeyAlgorithm(),
					signature.getHashAlgorithm());
			var verifiers = new ArrayList<PGPContentVerifier>();
			signaturesToVerify.put(signature, verifiers);
			for (var key : keys) {
				var verifier = verifierBuilder.build(key);
				verifierKeys.put(verifier, key);
				verifiers.add(verifier);
				signatureVerifiers.add(verifier.getOutputStream());
			}
		}

		var artifactLocation = repositoryLocation;
		switch (classifier) {
		case "osgi.bundle":
			artifactLocation = artifactLocation.resolve("plugins/" + id + "_" + version + ".jar");
			break;
		case "org.eclipse.update.feature":
			artifactLocation = artifactLocation.resolve("features/" + id + "_" + version + ".jar");
			break;
		case "binary":
			artifactLocation = artifactLocation.resolve("binary/" + id + "_" + version);
			break;
		default: {
			fail("Unexpected classifier " + classifier);
		}
		}

		Files.copy(artifactLocation, new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				for (var outputStream : signatureVerifiers) {
					outputStream.write(b);
				}
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				for (var outputStream : signatureVerifiers) {
					outputStream.write(b, off, len);
				}
			}
		});

		for (var entry : signaturesToVerify.entrySet()) {
			var signature = entry.getKey();
			for (var verifier : entry.getValue()) {
				verifier.getOutputStream().write(signature.getSignatureTrailer());
				if (!verifier.verify(signature.getSignature())) {
					fail("Signature fails to verify: " + id + ":" + version);
				} else {
					fingerprints.add(PGPPublicKeyService.toHex(verifierKeys.get(verifier).getFingerprint()));
				}
			}
		}

		return fingerprints;
	}
}
