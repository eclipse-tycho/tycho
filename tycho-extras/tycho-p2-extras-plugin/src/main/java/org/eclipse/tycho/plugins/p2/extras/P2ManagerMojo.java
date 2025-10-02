/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspace;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.eclipse.tycho.osgi.framework.Features;
import org.osgi.framework.BundleException;

/**
 * This goal wraps the P2 Manager application from JustJ Tools to maintain,
 * update, and manage the integrity of a public update site.
 * 
 * @see <a href="https://eclipse.dev/justj/?page=tools">JustJ P2 Manager</a>
 */
@Mojo(name = "p2-manager", threadSafe = true, requiresProject = false)
public class P2ManagerMojo extends AbstractMojo {

	@Component
	private EclipseWorkspaceManager workspaceManager;

	@Component
	private EclipseApplicationManager applicationManager;

	/**
	 * The repository where the P2 Manager application should be sourced from
	 */
	@Parameter
	private Repository managerRepository;

	/**
	 * Whether to print progress during the activities (opposite of -quiet flag)
	 */
	@Parameter(property = "p2manager.verbose", defaultValue = "true")
	private boolean verbose;

	/**
	 * The root folder of the project's update site
	 */
	@Parameter(property = "p2manager.root", required = true)
	private File root;

	/**
	 * Whether to show only the latest version of each installable unit
	 */
	@Parameter(property = "p2manager.latestVersionOnly")
	private boolean latestVersionOnly;

	/**
	 * Number of nightly builds to retain
	 */
	@Parameter(property = "p2manager.retain", defaultValue = "7")
	private int retain;

	/**
	 * The project label to use in generated pages
	 */
	@Parameter(property = "p2manager.label", defaultValue = "Project")
	private String label;

	/**
	 * Build URL for reference in generated pages
	 */
	@Parameter(property = "p2manager.buildUrl")
	private String buildUrl;

	/**
	 * Relative target folder within the root
	 */
	@Parameter(property = "p2manager.relative")
	private String relative;

	/**
	 * Remote location for repository operations
	 */
	@Parameter(property = "p2manager.remote")
	private String remote;

	/**
	 * Source repository URI to promote
	 */
	@Parameter(property = "p2manager.promote")
	private String promote;

	/**
	 * Folder containing products to promote
	 */
	@Parameter(property = "p2manager.promoteProducts")
	private File promoteProducts;

	/**
	 * List of download links to include in generated pages
	 */
	@Parameter(property = "p2manager.downloads")
	private List<String> downloads;

	/**
	 * Build timestamp in format yyyyMMddHHmm
	 */
	@Parameter(property = "p2manager.timestamp")
	private String timestamp;

	/**
	 * Build type (nightly, milestone, or release)
	 */
	@Parameter(property = "p2manager.type", defaultValue = "nightly")
	private String type;

	/**
	 * Favicon URL for generated pages
	 */
	@Parameter(property = "p2manager.favicon", defaultValue = "https://www.eclipse.org/eclipse.org-common/themes/solstice/public/images/favicon.ico")
	private String favicon;

	/**
	 * Title image URL for generated pages
	 */
	@Parameter(property = "p2manager.titleImage", defaultValue = "https://www.eclipse.org/eclipse.org-common/themes/solstice/public/images/logo/eclipse-426x100.png")
	private String titleImage;

	/**
	 * Body image URL for generated pages
	 */
	@Parameter(property = "p2manager.bodyImage")
	private String bodyImage;

	/**
	 * Target URL for generated pages
	 */
	@Parameter(property = "p2manager.targetUrl")
	private String targetUrl;

	/**
	 * Baseline URL for comparison
	 */
	@Parameter(property = "p2manager.baselineUrl")
	private String baselineUrl;

	/**
	 * Installable unit to use for version determination
	 */
	@Parameter(property = "p2manager.versionIU")
	private String versionIU;

	/**
	 * Pattern to match IU for version determination
	 */
	@Parameter(property = "p2manager.versionIUPattern")
	private String versionIUPattern;

	/**
	 * Pattern to filter installable units
	 */
	@Parameter(property = "p2manager.iuFilterPattern")
	private String iuFilterPattern;

	/**
	 * Pattern to filter primary installable units
	 */
	@Parameter(property = "p2manager.primaryIUFilterPattern", defaultValue = ".*\\.sdk([_.-]feature)?\\.feature\\.group")
	private String primaryIUFilterPattern;

	/**
	 * Pattern to exclude categories
	 */
	@Parameter(property = "p2manager.excludedCategoriesPattern")
	private String excludedCategoriesPattern;

	/**
	 * Git commit identifier
	 */
	@Parameter(property = "p2manager.commit")
	private String commit;

	/**
	 * Super target folder
	 */
	@Parameter(property = "p2manager.super")
	private File superTargetFolder;

	/**
	 * Whether to use SimRel alias
	 */
	@Parameter(property = "p2manager.simrelAlias")
	private boolean simrelAlias;

	/**
	 * Whether to generate BREE information
	 */
	@Parameter(property = "p2manager.bree")
	private boolean bree;

	/**
	 * Summary level (0 = none)
	 */
	@Parameter(property = "p2manager.summary", defaultValue = "0")
	private int summary;

	/**
	 * Pattern for summary IU filtering
	 */
	@Parameter(property = "p2manager.summaryIUPattern", defaultValue = ".*(?<!\\.source|\\.feature\\.group|\\.feature\\.jar)")
	private String summaryIUPattern;

	/**
	 * Maven wrapped mappings (pattern->replacement)
	 */
	@Parameter(property = "p2manager.mavenWrappedMappings")
	private List<String> mavenWrappedMappings;

	/**
	 * Name mappings (key->value)
	 */
	@Parameter(property = "p2manager.mappings")
	private List<String> mappings;

	/**
	 * Commit mappings (pattern->url)
	 */
	@Parameter(property = "p2manager.commitMappings")
	private List<String> commitMappings;

	/**
	 * Breadcrumbs for navigation
	 */
	@Parameter(property = "p2manager.breadcrumbs")
	private List<String> breadcrumbs;

	/**
	 * Archives to include
	 */
	@Parameter(property = "p2manager.archives")
	private List<String> archives;

	/**
	 * Paths to exclude
	 */
	@Parameter(property = "p2manager.excludes")
	private List<String> excludes;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (root == null) {
			throw new MojoFailureException("The 'root' parameter must be specified");
		}

		MavenRepositoryLocation repository = EclipseApplicationManager.getRepository(managerRepository,
				URI.create("https://download.eclipse.org/justj/tools/updates/"));
		EclipseApplication application = applicationManager.getApplication(repository, Bundles.of(), Features.of(),
				"P2 Manager");
		application.addFeature("org.eclipse.justj.p2.feature.group");
		EclipseWorkspace<?> workspace = workspaceManager.getWorkspace(repository.getURL(), this);

		List<String> arguments = new ArrayList<>();
		arguments.add(EclipseApplication.ARG_APPLICATION);
		arguments.add("org.eclipse.justj.p2.manager");
		arguments.add("-consoleLog");

		// Add flags
		if (!verbose) {
			arguments.add("-quiet");
		}
		if (latestVersionOnly) {
			arguments.add("-latestVersionOnly");
		}
		if (simrelAlias) {
			arguments.add("-simrel-alias");
		}
		if (bree) {
			arguments.add("-bree");
		}

		// Add parameters with values
		arguments.add("-root");
		arguments.add(root.getAbsolutePath());

		arguments.add("-retain");
		arguments.add(String.valueOf(retain));

		arguments.add("-label");
		arguments.add(label);

		arguments.add("-type");
		arguments.add(type);

		arguments.add("-favicon");
		arguments.add(favicon);

		arguments.add("-title-image");
		arguments.add(titleImage);

		arguments.add("-primary-iu-filter-pattern");
		arguments.add(primaryIUFilterPattern);

		arguments.add("-summary");
		arguments.add(String.valueOf(summary));

		arguments.add("-summary-iu-pattern");
		arguments.add(summaryIUPattern);

		// Add optional parameters
		addOptionalParameter(arguments, "-build-url", buildUrl);
		addOptionalParameter(arguments, "-relative", relative);
		addOptionalParameter(arguments, "-remote", remote);
		addOptionalParameter(arguments, "-promote", promote);
		addOptionalFile(arguments, "-promote-products", promoteProducts);
		addOptionalParameter(arguments, "-timestamp", timestamp);
		addOptionalParameter(arguments, "-body-image", bodyImage);
		addOptionalParameter(arguments, "-target-url", targetUrl);
		addOptionalParameter(arguments, "-baseline-url", baselineUrl);
		addOptionalParameter(arguments, "-version-iu", versionIU);
		addOptionalParameter(arguments, "-version-iu-pattern", versionIUPattern);
		addOptionalParameter(arguments, "-iu-filter-pattern", iuFilterPattern);
		addOptionalParameter(arguments, "-excluded-categories-pattern", excludedCategoriesPattern);
		addOptionalParameter(arguments, "-commit", commit);
		addOptionalFile(arguments, "-super", superTargetFolder);

		// Add list parameters
		addListParameter(arguments, "-downloads", downloads);
		addListParameter(arguments, "-maven-wrapped-mapping", mavenWrappedMappings);
		addListParameter(arguments, "-mapping", mappings);
		addListParameter(arguments, "-commit-mapping", commitMappings);
		addListParameter(arguments, "-breadcrumb", breadcrumbs);
		addListParameter(arguments, "-archive", archives);
		addListParameter(arguments, "--exclude", excludes);

		getLog().info("Calling P2 Manager application with arguments: " + arguments);
		try (EclipseFramework framework = application.startFramework(workspace, arguments)) {
			framework.start();
		} catch (BundleException e) {
			throw new MojoFailureException("Can't start framework!", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause.getClass().getName().equals(CoreException.class.getName())
					|| cause.getClass().getName().equals(ProvisionException.class.getName())) {
				throw new MojoFailureException(cause.getMessage(), cause);
			}
			throw new MojoExecutionException(cause);
		} catch (Exception e) {
			throw new MojoExecutionException(e);
		}
	}

	private void addOptionalParameter(List<String> arguments, String name, String value) {
		if (value != null && !value.isEmpty()) {
			arguments.add(name);
			arguments.add(value);
		}
	}

	private void addOptionalFile(List<String> arguments, String name, File file) {
		if (file != null) {
			arguments.add(name);
			arguments.add(file.getAbsolutePath());
		}
	}

	private void addListParameter(List<String> arguments, String name, List<String> values) {
		if (values != null && !values.isEmpty()) {
			for (String value : values) {
				if (value != null && !value.trim().isEmpty()) {
					arguments.add(name);
					arguments.add(value.trim());
				}
			}
		}
	}
}
