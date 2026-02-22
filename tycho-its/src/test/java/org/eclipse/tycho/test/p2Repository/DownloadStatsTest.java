/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class DownloadStatsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testNoDownloadStatsByDefault() throws Exception {
		Verifier verifier = getVerifier("p2Repository.reactor", false);
		verifier.addCliArgument("-De352-repo=" + P2Repositories.ECLIPSE_352.toString());

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		File artifactXml = new File(verifier.getBasedir(), "eclipse-repository/target/repository/artifacts.xml");
		assertTrue(captureDownloadStatsFromArtifactsXML(artifactXml, null).isEmpty());
	}

	@Test
	public void testDownloadStatsAddedUponProperty() throws Exception {
		Verifier verifier = getVerifier("p2Repository.reactor", false);
		verifier.addCliArgument("-De352-repo=" + P2Repositories.ECLIPSE_352.toString());
		verifier.addCliArgument("-Dtycho.generateDownloadStatsProperty=true");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		File artifactXml = new File(verifier.getBasedir(), "eclipse-repository/target/repository/artifacts.xml");
		assertEquals(4, captureDownloadStatsFromArtifactsXML(artifactXml, null).size());
	}

	public static List<String> captureDownloadStatsFromArtifactsJar(JarFile artifactJar,
			Predicate<Element> elementFilter) throws IOException, FileNotFoundException {
		List<String> downloadStats = new ArrayList<>();
		try (InputStream stream = artifactJar.getInputStream(artifactJar.getJarEntry("artifacts.xml"))) {
			downloadStats.addAll(captureDownloadStatsFromArtifactsXml(new XMLParser().parse(new XMLIOSource(stream)),
					elementFilter));
		}
		return downloadStats;
	}

	private static List<String> captureDownloadStatsFromArtifactsXml(Document document,
			Predicate<Element> elementFilter) {
		if (elementFilter == null) {
			elementFilter = o -> true;
		}
		List<String> downloadStats = new ArrayList<>();
		for (Element element : document.getChild("repository").getChild("artifacts").getChildren("artifact")) {
			if (elementFilter.test(element)) {
				for (Element property : element.getChild("properties").getChildren("property")) {
					if (property.getAttributeValue("name").equals("download.stats")) {
						downloadStats.add(property.getAttributeValue("value"));
					}
				}
			}
		}
		return downloadStats;
	}

	public static List<String> captureDownloadStatsFromArtifactsXML(File artifactsXml, Predicate<Element> elementFilter)
			throws IOException {
		return captureDownloadStatsFromArtifactsXml(XMLParser.parse(artifactsXml), elementFilter);
	}
}
