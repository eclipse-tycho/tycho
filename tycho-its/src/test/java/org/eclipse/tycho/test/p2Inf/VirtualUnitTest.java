/*******************************************************************************
 * Copyright (c) 2023 Martin D'Aloia and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Inf;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

/**
 * Test that a virtual IU created just with metadata in a p2.inf file can be
 * required in the same p2.inf file.
 * <p>
 * It used to work until 3.0.5 (using
 * <code><pomDependencies>consider</pomDependencies></code>) but since 4.0.0
 * failed with:
 * 
 * <pre>
 * Cannot resolve project dependencies:
 *   Software being installed: pvu.bundle 1.0.0.qualifier
 *   Missing requirement: pvu.bundle 1.0.0.qualifier requires 'org.eclipse.equinox.p2.iu; configure.pvu.bundle 0.0.0' but it could not be found
 * </pre>
 *
 * See
 * https://github.com/eclipse-tycho/tycho/blob/master/RELEASE_NOTES.md#mixed-reactor-setups-require-the-new-resolver-now
 * and https://github.com/eclipse-tycho/tycho/issues/2977
 */
public class VirtualUnitTest extends AbstractTychoIntegrationTest {

	@Test
	public void testVirtualUnitRequirementDoesNotFailBuild() throws Exception {
		Verifier verifier = getVerifier("/p2Inf.virtualUnit", false);
		verifier.executeGoals(asList("verify"));
		verifier.verifyErrorFreeLog();

		String hostUnitId = "pvu.bundle";
		String configureUnitId = "configure.pvu.bundle";

		List<Element> units = getUnits(verifier.getBasedir(), "bundle/target/p2content.xml");
		Optional<Element> hostUnit = findUnit(units, hostUnitId);
		Optional<Element> configureUnit = findUnit(units, configureUnitId);

		Stream<Element> hostUnitRequirements = findRequirements(hostUnit);

		assertTrue("Host IU " + hostUnitId + " not found", hostUnit.isPresent());
		assertTrue("Configure IU " + configureUnitId + " not found", configureUnit.isPresent());
		assertTrue("Requirement of IU " + configureUnitId + " not found in IU " + hostUnitId,
				hostUnitRequirements.anyMatch(elem -> configureUnitId.equals(elem.getAttributeValue("name"))));
	}

	@Test
	public void testVirtualUnitMultiBundleWithRequirementDoesNotFailBuild() throws Exception {
		Verifier verifier = getVerifier("/p2Inf.virtualUnit.multiBundle", false);
		verifier.executeGoals(asList("verify"));
		verifier.verifyErrorFreeLog();

		// Host bundle and virtual IU assertions
		String hostUnitId = "pvumb.bundle1";
		String configureUnitId = "configure.pvumb.bundle1";

		List<Element> units = getUnits(verifier.getBasedir(), "bundle1/target/p2content.xml");
		Optional<Element> hostUnit = findUnit(units, hostUnitId);
		Optional<Element> configureUnit = findUnit(units, configureUnitId);

		Stream<Element> hostUnitRequirements = findRequirements(hostUnit);

		assertTrue("Host IU " + hostUnitId + " not found", hostUnit.isPresent());
		assertTrue("Configure IU " + configureUnitId + " not found", configureUnit.isPresent());
		assertTrue("Requirement of IU " + configureUnitId + " not found in IU " + hostUnitId,
				hostUnitRequirements.anyMatch(elem -> configureUnitId.equals(elem.getAttributeValue("name"))));

		// Client bundle assertions
		String clientUnitId = "pvumb.bundle2";

		units = getUnits(verifier.getBasedir(), "bundle2/target/p2content.xml");
		Optional<Element> clientUnit = findUnit(units, clientUnitId);

		Stream<Element> clientUnitRequirements = findRequirements(clientUnit);

		assertTrue("Client IU " + clientUnitId + " not found", clientUnit.isPresent());
		assertTrue("Requirement of IU " + hostUnitId + " not found in IU " + clientUnitId,
				clientUnitRequirements.anyMatch(elem -> hostUnitId.equals(elem.getAttributeValue("name"))));
	}

	private static List<Element> getUnits(String baseDir, String filePath) throws IOException {
		File p2Content = new File(baseDir, filePath);
		Document doc = XMLParser.parse(p2Content);

		return doc.getChild("units").getChildren("unit");
	}

	private static Optional<Element> findUnit(List<Element> units, String hostUnitId) {
		return units.stream().filter(elem -> hostUnitId.equals(elem.getAttributeValue("id"))).findFirst();
	}

	private static Stream<Element> findRequirements(Optional<Element> hostUnit) {
		return hostUnit.stream().flatMap(elem -> elem.getChild("requires").getChildren("required").stream());
	}

}
