/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.buildtimestamp.jgit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JGitBuildTimestampProviderTest {

	private static final Instant BUNDLE_COMMIT = Instant.parse("2024-01-01T10:00:00Z");
	private static final Instant WEB_COMMIT = Instant.parse("2024-02-01T10:00:00Z");

	@TempDir
	Path tempDir;

	private Path repository;
	private MavenProject bundle;
	private final JGitBuildTimestampProvider provider = new JGitBuildTimestampProvider();

	@BeforeEach
	void createRepositoryWhereOnlyWebSourcesChangedLast() throws Exception {
		repository = tempDir.resolve("repo");
		// built without reading GIT_DIR or GIT_INDEX_FILE, so it never writes into the developer's repository
		try (Repository repo = new FileRepositoryBuilder().setWorkTree(repository.toFile())
				.setGitDir(repository.resolve(".git").toFile()).build(); Git git = new Git(repo)) {
			repo.create();
			commit(git, BUNDLE_COMMIT, "bundle/pom.xml", "other/file.txt", "web/src/view.js", "web/README.md");
			commit(git, WEB_COMMIT, "web/src/view.js");
		}
		bundle = new MavenProject();
		bundle.setFile(repository.resolve("bundle/pom.xml").toFile());
	}

	@Test
	void ignoresChangesOutsideBasedirByDefault() throws Exception {
		assertEquals(date(BUNDLE_COMMIT), provider.getTimestamp(null, bundle, execution(config())));
	}

	@Test
	void considersCommitsInRelativeAdditionalPath() throws Exception {
		assertEquals(date(WEB_COMMIT), provider.getTimestamp(null, bundle,
				execution(config("jgit.additionalPaths", "../web"))));
	}

	@Test
	void considersCommitsInAnyOfSeveralAdditionalPathsIncludingAbsoluteOnes() throws Exception {
		String webPath = repository.resolve("web/src").toAbsolutePath().toString();
		assertEquals(date(WEB_COMMIT), provider.getTimestamp(null, bundle,
				execution(config("jgit.additionalPaths", "../other\r\n  " + webPath + "  \n"))));
	}

	@Test
	void appliesIgnorePatternsToAdditionalPaths() throws Exception {
		try (Git git = Git.open(repository.toFile())) {
			commit(git, Instant.parse("2024-03-01T10:00:00Z"), "web/README.md");
		}
		Xpp3Dom config = config("jgit.additionalPaths", "../web");
		config.addChild(element("jgit.ignore", "*.md"));
		assertEquals(date(WEB_COMMIT), provider.getTimestamp(null, bundle, execution(config)));
	}

	@Test
	void failsOnDirtyAdditionalPath() throws Exception {
		Files.writeString(repository.resolve("web/src/view.js"), "modified");
		assertEquals(date(BUNDLE_COMMIT), provider.getTimestamp(null, bundle, execution(config())));
		MojoExecutionException e = assertThrows(MojoExecutionException.class,
				() -> provider.getTimestamp(null, bundle, execution(config("jgit.additionalPaths", "../web"))));
		assertTrue(e.getMessage().contains("web/src/view.js"), e.getMessage());
	}

	@Test
	void failsOnMissingAdditionalPath() {
		MojoExecutionException e = assertThrows(MojoExecutionException.class,
				() -> provider.getTimestamp(null, bundle, execution(config("jgit.additionalPaths", "../missing"))));
		assertTrue(e.getMessage().contains("does not exist"), e.getMessage());
	}

	@Test
	void failsOnAdditionalPathOutsideRepository() throws IOException {
		Files.createDirectories(tempDir.resolve("repo-sibling"));
		MojoExecutionException e = assertThrows(MojoExecutionException.class, () -> provider.getTimestamp(null,
				bundle, execution(config("jgit.additionalPaths", "../../repo-sibling"))));
		assertTrue(e.getCause().getMessage().contains("is not in git repository working tree"),
				e.getCause().getMessage());
	}

	private void commit(Git git, Instant when, String... paths) throws Exception {
		for (String path : paths) {
			Path file = repository.resolve(path);
			Files.createDirectories(file.getParent());
			Files.writeString(file, path + " at " + when);
			git.add().addFilepattern(path).call();
		}
		PersonIdent ident = new PersonIdent("Tycho", "tycho@example.org", when, ZoneOffset.UTC);
		// independent of the developer's global commit signing and pre-commit/commit-msg hooks
		git.commit().setSign(false).setNoVerify(true).setMessage("change at " + when).setAuthor(ident)
				.setCommitter(ident).call();
	}

	private static Date date(Instant instant) {
		return Date.from(instant);
	}

	private static Xpp3Dom config(String... nameAndValue) {
		Xpp3Dom config = new Xpp3Dom("configuration");
		for (int i = 0; i < nameAndValue.length; i += 2) {
			config.addChild(element(nameAndValue[i], nameAndValue[i + 1]));
		}
		return config;
	}

	private static Xpp3Dom element(String name, String value) {
		Xpp3Dom element = new Xpp3Dom(name);
		element.setValue(value);
		return element;
	}

	private static MojoExecution execution(Xpp3Dom configuration) {
		Plugin plugin = new Plugin();
		plugin.setConfiguration(configuration);
		return new MojoExecution(plugin, "build-qualifier", "default");
	}
}
