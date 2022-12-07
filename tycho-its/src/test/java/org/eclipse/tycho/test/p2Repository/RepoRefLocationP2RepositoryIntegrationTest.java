/*******************************************************************************
 * Copyright (c) 2019, 2021 Guillaume Dufour and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Guillaume Dufour - support repo ref location (453708)
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

public class RepoRefLocationP2RepositoryIntegrationTest extends AbstractTychoIntegrationTest {

	private static Verifier verifier;

	private static class RepositoryReferenceData {
		private String uri;
		private String type;
		private String enabled;

		public RepositoryReferenceData(String uri, String type, String enabled) {
			this.uri = uri;
			this.type = type;
			this.enabled = enabled;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RepositoryReferenceData other = (RepositoryReferenceData) obj;
			return Objects.equals(enabled, other.enabled) && Objects.equals(type, other.type)
					&& Objects.equals(uri, other.uri);
		}

		@Override
		public String toString() {
			return "[uri=" + uri + ", type=" + type + ", enabled=" + enabled + "]";
		}
	}

	@BeforeClass
	public static void executeBuild() throws Exception {
		verifier = new RepoRefLocationP2RepositoryIntegrationTest().getVerifier("/p2Repository.repositoryRef.location",
				false);
		verifier.addCliArgument("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testRefLocation() throws Exception {
		File target = new File(verifier.getBasedir(), "target");
		File repository = new File(target, "repository");
		File contentXml = new File(repository, "content.xml");
		assertTrue(contentXml.isFile());
		File artifactXml = new File(repository, "artifacts.xml");
		assertTrue(artifactXml.isFile());
		assertTrue(new File(target, "category.xml").isFile());

		Document artifactsDocument = XMLParser.parse(contentXml);
		// See MetadataRepositoryIO.Writer#writeRepositoryReferences
		List<Element> repositories = artifactsDocument.getChild("repository").getChild("references")
				.getChildren("repository");
		assertEquals(4, repositories.size());
		List<RepositoryReferenceData> actual = repositories.stream()
				.map(e -> new RepositoryReferenceData(e.getAttributeValue("uri"), e.getAttributeValue("type"),
						e.getAttributeValue("options")))
				.toList();
		assertThat(actual,
				containsInAnyOrder(new RepositoryReferenceData("http://some.where", "1", "0"),
						new RepositoryReferenceData("http://some.where", "0", "0"),
						new RepositoryReferenceData("http://some.where.else", "1", "1"),
						new RepositoryReferenceData("http://some.where.else", "0", "1")));
	}

}
