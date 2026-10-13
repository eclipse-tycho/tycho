/*******************************************************************************
 * Copyright (c) 2025, 2026 Christoph Läubrich and others.
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

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationFactory;
import org.eclipse.tycho.osgi.framework.EclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspace;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.eclipse.tycho.osgi.framework.Features;
import org.osgi.framework.BundleException;

/**
 * This goal wraps the P2 Manager application from JustJ Tools to maintain, update, and manage the
 * integrity of a public update site.
 *
 * @see <a href="https://eclipse.dev/justj/?page=tools">JustJ P2 Manager</a>
 */
@Mojo(name = "p2-manager", threadSafe = true, requiresProject = false)
public class P2ManagerMojo extends AbstractMojo {
    private static final String JUST_TOOLS_NIGHTLY = "https://download.eclipse.org/justj/tools/updates/nightly/latest";

    @Inject
    private EclipseWorkspaceManager workspaceManager;

    @Inject
    private EclipseApplicationManager applicationManager;

    @Inject
    private EclipseApplicationFactory applicationFactory;

    /**
     * The repository from which the P2 Manager application should be sourced.
     * 
     * <pre>
     * &lt;managerRepository>
     *   &lt;id>eclipse.justj.tools.eclipse.repo&lt;/id>
     *   &lt;layout>p2&lt;</layout>
     *   &lt;url>download.eclipse.org/justj/tools/updates/nightly/latest/&lt;/url>
     * &lt;/managerRepository>
     * </pre>
     */
    @Parameter
    private Repository managerRepository;

    /**
     * The repository from which the Eclipse runtime should be sourced.
     * 
     * <pre>
     * &lt;eclipseRepository>
     *   &lt;id>eclipse.justj.tools.eclipse.repo&lt;/id>
     *   &lt;layout>p2&lt;</layout>
     *   &lt;url>https://download.eclipse.org/releases/latest&lt;/url>
     * &lt;/eclipseRepository>
     * </pre>
     */
    @Parameter
    private Repository eclipseRepository;

    /**
     * Whether to print progress during the activities; the opposite of <code>-quiet</code> flag.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#quiet">p2.manager -quiet</a>
     */
    @Parameter(property = "p2manager.verbose", defaultValue = "true")
    private boolean verbose;

    /**
     * The root folder of the project's update site.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#root">p2.manager -root</a>
     */
    @Parameter(property = "p2manager.root", required = true)
    private File root;

    /**
     * Whether to promote only the latest version of each installable unit.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#latestVersionOnly">p2.manager
     *      -latestVersionOnly</a>
     */
    @Parameter(property = "p2manager.latestVersionOnly")
    private boolean latestVersionOnly;

    /**
     * The number of nightly builds to retain.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#retain">p2.manager -retain</a>
     */
    @Parameter(property = "p2manager.retain", defaultValue = "7")
    private int retain;

    /**
     * The project label to use in the generated pages.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#label">p2.manager -label</a>
     */
    @Parameter(property = "p2manager.label", defaultValue = "Project")
    private String label;

    /**
     * The build URL for reference in the generated pages.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#build-url">p2.manager -build-url</a>
     */
    @Parameter(property = "p2manager.buildUrl")
    private String buildUrl;

    /**
     * The relative target folder within the root.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#relative">p2.manager -relative</a>
     */
    @Parameter(property = "p2manager.relative", defaultValue = "updates")
    private String relative;

    /**
     * The remote location for repository operations.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#remote">p2.manager -remote</a>
     */
    @Parameter(property = "p2manager.remote")
    private String remote;

    /**
     * The source repository URI to promote.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#promote">p2.manager -promote</a>
     */
    @Parameter(property = "p2manager.promote")
    private String promote;

    /**
     * The folder containing products to promote.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#promote-products">p2.manager
     *      -promote-products</a>
     */
    @Parameter(property = "p2manager.promoteProducts")
    private File promoteProducts;

    /**
     * The list of download links to include in the generated pages.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#downloads">p2.manager -downloads</a>
     */
    @Parameter(property = "p2manager.downloads")
    private List<String> downloads;

    /**
     * The build timestamp in the format <code>yyyyMMddHHmm</code>.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#timestamp">p2.manager -timestamp</a>
     */
    @Parameter(property = "p2manager.timestamp")
    private String timestamp;

    /**
     * The build type, i.e., nightly, milestone, or release.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#type">p2.manager -type</a>
     */
    @Parameter(property = "p2manager.type", defaultValue = "nightly")
    private String type;

    /**
     * The favicon URL for generated pages.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#favicon">p2.manager -favicon</a>
     */
    @Parameter(property = "p2manager.favicon", defaultValue = "https://www.eclipse.org/eclipse.org-common/themes/solstice/public/images/favicon.ico")
    private String favicon;

    /**
     * The title image URL for generated pages.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#title-image">p2.manager -title-image</a>
     */
    @Parameter(property = "p2manager.titleImage", defaultValue = "https://www.eclipse.org/eclipse.org-common/themes/solstice/public/images/logo/eclipse-426x100.png")
    private String titleImage;

    /**
     * The body image URL for generated pages.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#body-image">p2.manager -body-image</a>
     */
    @Parameter(property = "p2manager.bodyImage")
    private String bodyImage;

    /**
     * The target URL where the generated pages will be hosted after promotion.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#target-url">p2.manager -target-url</a>
     */
    @Parameter(property = "p2manager.targetUrl")
    private String targetUrl;

    /**
     * The baseline URL for comparison and replacement.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#baseline-url">p2.manager
     *      -baseline-url</a>
     */
    @Parameter(property = "p2manager.baselineUrl")
    private String baselineUrl;

    /**
     * The installable unit to use for version determination.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#version-iu">p2.manager -version-iu</a>
     */
    @Parameter(property = "p2manager.versionIU")
    private String versionIU;

    /**
     * The pattern to match installable units for version determination.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#version-iu-pattern">p2.manager
     *      -version-iu-pattern</a>
     */
    @Parameter(property = "p2manager.versionIUPattern")
    private String versionIUPattern;

    /**
     * The pattern to filter the installable units displayed in the generated index.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#iu-filter-pattern">p2.manager
     *      -iu-filter-pattern</a>
     */
    @Parameter(property = "p2manager.iuFilterPattern")
    private String iuFilterPattern;

    /**
     * The pattern to filter the primary installable units displayed in the generated index.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#primary-iu-filter-pattern">p2.manager
     *      -primary-iu-filter-pattern</a>
     */
    @Parameter(property = "p2manager.primaryIUFilterPattern", defaultValue = ".*\\.sdk([_.-]feature)?\\.feature\\.group")
    private String primaryIUFilterPattern;

    /**
     * The pattern to exclude categories from being promoted.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#excluded-categories-pattern">p2.manager
     *      -excluded-categories-pattern</a>
     */
    @Parameter(property = "p2manager.excludedCategoriesPattern")
    private String excludedCategoriesPattern;

    /**
     * The Git commit identifier.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#commit">p2.manager -commit</a>
     */
    @Parameter(property = "p2manager.commit")
    private String commit;

    /**
     * The super target folder; don't use this.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#super">p2.manager -super</a>
     */
    @Parameter(property = "p2manager.super")
    private File superTargetFolder;

    /**
     * Whether to generate a SimRel alias repository; use only if the version numbers follow those
     * of the Eclipse Platform releases.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#simrel-alias">p2.manager
     *      -simrel-alias</a>
     */
    @Parameter(property = "p2manager.simrelAlias")
    private boolean simrelAlias;

    /**
     * Whether to generate BREE information.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#bree">p2.manager -bree</a>
     */
    @Parameter(property = "p2manager.bree")
    private boolean bree;

    /**
     * The summary level; 0 = no generated summary table.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#summary">p2.manager -summary</a>
     */
    @Parameter(property = "p2manager.summary", defaultValue = "0")
    private int summary;

    /**
     * The pattern for summary installable unit filtering.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#summary-iu-pattern">p2.manager
     *      -summary-iu-pattern</a>
     */
    @Parameter(property = "p2manager.summaryIUPattern", defaultValue = ".*(?<!\\.source|\\.feature\\.group|\\.feature\\.jar)")
    private String summaryIUPattern;

    /**
     * The Maven wrapped mappings, i.e., <code>pattern -> replacement</code>.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#maven-wrapped-mapping">p2.manager
     *      -maven-wrapped-mapping</a>
     */
    @Parameter(property = "p2manager.mavenWrappedMapping")
    private List<String> mavenWrappedMappings;

    /**
     * The name mappings for proper title-case conversion, i.e., <code>key -> value</code>.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#mapping">p2.manager -mapping</a>
     */
    @Parameter(property = "p2manager.mappings")
    private List<String> mappings;

    /**
     * The commit mappings used if the repository location has been migrated, i.e.,
     * <code>pattern -> url</code>.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#commit-mapping">p2.manager
     *      -commit-mapping</a>
     */
    @Parameter(property = "p2manager.commitMappings")
    private List<String> commitMappings;

    /**
     * The breadcrumbs for the navigation bar.
     * 
     * <pre>
     * &lt;breadcrumbs>
     *   &lt;breadcrumb>m2e&amp;#xA0;WTP https://projects.eclipse.org/projects/technology.m2e&lt;/breadcrumb>
     * &lt;/breadcrumbs>
     * </pre>
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#breadcrumb-arg">p2.manager
     *      -breadcrumb</a>
     */
    @Parameter(property = "p2manager.breadcrumbs")
    private List<String> breadcrumbs;

    /**
     * The archives to include.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#archive">p2.manager -archive</a>
     */
    @Parameter(property = "p2manager.archive")
    private List<String> archives;

    /**
     * The paths to exclude from promotion.
     *
     * @see <a href="https://eclipse.dev/justj/?page=tools#exclude">p2.manager -exclude</a>
     */
    @Parameter(property = "p2manager.excludes")
    private List<String> excludes;

    /**
     * The bundle symbolic name of the ECF file transfer provider to be used by the application.
     */
    @Parameter(property = "p2manager.ecfProvider", defaultValue = "org.eclipse.ecf.provider.filetransfer.httpclientjava")
    private String ecfProvider;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (root == null) {
            throw new MojoFailureException("The 'root' parameter must be specified");
        }

        MavenRepositoryLocation eclipseLocation = getEclipseLocation();
        MavenRepositoryLocation repository = getManagerLocation();
        TargetPlatform targetPlatform = applicationFactory.createTargetPlatform(List.of(eclipseLocation, repository));
        EclipseApplication application = applicationManager.getApplication(targetPlatform, Bundles.of(), Features.of(),
                "P2 Manager");
        application.addBundle("org.eclipse.justj.p2");
        application.addBundle("org.apache.felix.scr");
        application.addBundle(ecfProvider);
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

        arguments.add("-relative");
        arguments.add(relative);

        // Add optional parameters
        addOptionalParameter(arguments, "-build-url", buildUrl);
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

    private MavenRepositoryLocation getManagerLocation() {
        if (managerRepository == null) {
            return new MavenRepositoryLocation(null, URI.create(JUST_TOOLS_NIGHTLY));
        }
        return new MavenRepositoryLocation(managerRepository.getId(), managerRepository.getLocation());
    }

    private MavenRepositoryLocation getEclipseLocation() {
        if (eclipseRepository == null) {
            return new MavenRepositoryLocation(null, URI.create(TychoConstants.ECLIPSE_LATEST));
        }
        return new MavenRepositoryLocation(eclipseRepository.getId(), eclipseRepository.getLocation());
    }
}
