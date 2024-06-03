/*******************************************************************************
 * Copyright (c) 2024 Martin D'Aloia and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;
import org.tukaani.xz.XZInputStream;

/**
 * Test that the goal `tycho-p2-repository:fix-artifacts-metadata` removes
 * old checksums if they are present in the source metadata.
 * New p2 libs are configured to not publish them anymore.
 * <p>
 * If not removed, they are checked during the product assembly and because
 * they continue to keep an old value (if a IU was modified for example to
 * (re)sign it) this step fails to complete due to checksum mismatch.
 *
 * See https://github.com/eclipse-tycho/tycho/issues/2875
 */
public class P2RepositoryFixArtifactsMetadataOldChecksumsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testRemoveOldChecksumsNotRecalculated() throws Exception {
		Verifier verifier = getVerifier("/p2Repository.fixArtifactsMetadata.oldChecksums", false);
		verifier.executeGoals(asList("verify"));
		verifier.verifyErrorFreeLog();

		Path repositoryPath = Path.of(verifier.getBasedir(), "target/repository");
		Path artifactPath = repositoryPath.resolve("artifacts.xml.xz");
		assertTrue(artifactPath.toFile().isFile());

		Xpp3Dom dom;
		try (XZInputStream stream = new XZInputStream(Files.newInputStream(artifactPath))) {
			dom = Xpp3DomBuilder.build(stream, StandardCharsets.UTF_8.displayName());
		} catch (IOException | XmlPullParserException e) {
			fail(e.getMessage());
			throw e;
		}

		Map<String, String> artifactProperties = getArtifactProperties(dom, "org.slf4j.api");

		String[] checksumsThatMustNotBePresent = {"download.md5", "download.checksum.md5"};
		for(String checksumKey : checksumsThatMustNotBePresent) {
			String checksumValue = artifactProperties.get(checksumKey);

			assertNull("Property '" + checksumKey + "' is present in artifacts metadata", checksumValue);
		}
	}

	private Map<String, String> getArtifactProperties(Xpp3Dom element, String artifactId) {
		return Arrays.stream(element.getChild("artifacts").getChildren())
				.filter(it -> artifactId.equals(it.getAttribute("id")))
				.flatMap(it -> Arrays.stream(it.getChild("properties").getChildren()))
				.collect(Collectors.toMap(it -> it.getAttribute("name"), it -> it.getAttribute("value")));
	}

}
