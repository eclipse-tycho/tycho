/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *    Christoph Läubrich - Christoph Läubrich - Issue #502 - TargetDefinitionUtil / UpdateTargetMojo should not be allowed to modify the internal state of the target
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.Segment;
import org.codehaus.mojo.versions.model.RuleSet;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.targetplatform.TargetPlatformArtifactResolver;
import org.eclipse.tycho.targetplatform.TargetResolveException;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;

/**
 * This allows to update a target file to use newer version of specified items, e.g. IUs from
 * updatesites or maven coordinates. In the simplest form this is called like
 *
 * <pre>
 * mvn -f [path to target project] tycho-version-bump:update-target
 * </pre>
 * <p>
 * For updating <b>maven target locations</b> the mojo supports
 * <a href="https://www.mojohaus.org/versions/versions-maven-plugin/version-rules.html">Version
 * number comparison rule-sets</a> similar to the
 * <a href="https://www.mojohaus.org/versions/versions-maven-plugin">Versions Maven Plugin</a>
 * please check the documentation there for further information about ruleset files.
 * </p>
 * <p>
 * For updating <b>installable unit locations</b> (also known as update sites) you can configure
 * different strategies (see {@link #getUpdateSiteDiscovery()}) to discover updates.
 * </p>
 */
@Mojo(name = "update-target")
public class UpdateTargetMojo extends AbstractUpdateMojo {
    /**
     * Specify the path to the target file to update, if not given the current project settings will
     * be used to find a suitable file
     */
    @Parameter(property = "target")
    private File targetFile;

    /**
     * Whether to allow the major version number to be changed.
     */
    @Parameter(property = "allowMajorUpdates", defaultValue = "true")
    private boolean allowMajorUpdates;

    /**
     * Whether to allow the minor version number to be changed.
     *
     */
    @Parameter(property = "allowMinorUpdates", defaultValue = "true")
    private boolean allowMinorUpdates;

    /**
     * Whether to allow the incremental version number to be changed.
     *
     */
    @Parameter(property = "allowIncrementalUpdates", defaultValue = "true")
    private boolean allowIncrementalUpdates;

    /**
     * Whether to allow the subIncremental version number to be changed.
     *
     */
    @Parameter(property = "allowSubIncrementalUpdates", defaultValue = "true")
    private boolean allowSubIncrementalUpdates;

    /**
     * A list of update site discovery strategies, the following is currently supported:
     * <ul>
     * <li><code>parent</code> - search the parent path for a composite that can be used to find
     * newer versions</li>
     * <li><code>versionPattern[:pattern]</code> - specifies a pattern to match in the URL (defaults
     * to <code>(\d+)\.(\d+)</code> where it increments each numeric part beginning at the last
     * group, if no repository is found using the next group setting the previous to zero. Any non
     * numeric pattern will be replaced by the empty string</li>
     * <li><code>datePattern[:pattern:format[:period]]</code> - specifies a pattern and format to
     * match in the URL (defaults to <code>(\d{4}-\d{2}):yyyy-MM</code>)</li> where the pattern
     * defines a date that should be incremented by a given {@link Period} (defaults to
     * <code>P3M7D</code>)</li>
     * </ul>
     * If used on the CLI, individual values must be separated by a comma see <a href=
     * "https://maven.apache.org/guides/mini/guide-configuring-plugins.html#Mapping_Collections_and_Arrays">here</a>
     */
    @Parameter(property = "discovery")
    private List<String> updateSiteDiscovery;

    /**
     * <p>
     * Allows specifying a {@linkplain RuleSet} object describing rules on maven artifact versions
     * to ignore when considering updates.
     * </p>
     */
    @Parameter
    private RuleSet mavenRuleSet;

    /**
     * <p>
     * Allows specifying ignored maven artifact versions as an alternative to providing a
     * {@linkplain #mavenRuleSet} parameter.
     * </p>
     */
    @Parameter(property = "maven.version.ignore")
    private Set<String> mavenIgnoredVersions;

    /**
     * URI of a ruleSet file containing the rules that control how to compare version numbers.
     */
    @Parameter(property = "maven.version.rules")
    private String mavenRulesUri;

    @Parameter(defaultValue = "Please review the changes and merge if appropriate, or cherry pick individual updates.", property = "tycho.updatetarget.report.preamble")
    private String reportPreamble;

    @Parameter(defaultValue = "${project.build.directory}/targetUpdates.md", property = "tycho.updatetarget.report")
    private File reportFileName;

    /**
     * If enabled, missing or empty versions are updated to the most recent found in the repository
     * to guarantee a stable build
     */
    @Parameter(defaultValue = "true", property = "tycho.updatetarget.updateEmptyVersion")
    private boolean updateEmptyVersion;

    @Component
    private MavenSession mavenSession;

    @Parameter(defaultValue = "${mojoExecution}", required = true, readonly = true)
    private MojoExecution mojoExecution;

    @Inject
    private MavenLocationUpdater mavenLocationUpdater;

    @Inject
    private InstallableUnitLocationUpdater installableUnitLocationUpdater;

    MarkdownBuilder builder;

    @Override
    protected void doUpdate(File file) throws Exception {
        getLog().info("Update target file " + file);
        //we use domtrip here because we need to retain the formatting of the original file
        Document target = Document.of(file.toPath());
        boolean changed = false;
        builder = new MarkdownBuilder(reportFileName);
        builder.h2("The content of the target `" + file.getName() + "` was updated");
        if (reportPreamble != null && !reportPreamble.isBlank()) {
            builder.add(reportPreamble);
            builder.newLine();
        }
        List<MavenVersionUpdate> mavenUpdates = new ArrayList<>();
        try (FileInputStream input = new FileInputStream(file)) {
            for (Element iuLocation : getLocations("InstallableUnit", target)) {
                changed |= installableUnitLocationUpdater.update(iuLocation, this);
            }
            for (Element mavenLocation : getLocations("Maven", target)) {
                mavenUpdates.addAll(mavenLocationUpdater.update(mavenLocation, this));
                changed |= mavenUpdates.size() > 0;
            }
        }
        if (changed) {
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
                target.toXml(os);
            } catch (IOException e) {
                throw new MojoFailureException("Failed to write updated target file", e);
            }
            if (mavenUpdates.size() > 0) {
                builder.h3("The following maven artifacts have been updated:");
                Set<String> updatedMsg = new HashSet<>();
                for (MavenVersionUpdate update : mavenUpdates) {
                    if (updatedMsg.add(update.id())) {
                        update.describeUpdate(builder);
                    }
                }
                builder.newLine();
            }
            builder.newLine();
            builder.write();
        }
        builder = null;
    }

    static void setElementValue(String name, String value, Element root) {
        Element child = root.child(name).orElse(null);
        if (child != null) {
            child.textContent(value);
        }
    }

    static String getElementValue(String name, Element root) {
        Element child = root.child(name).orElse(null);
        if (child != null) {
            String text = child.textContent().trim();
            if (text.isBlank()) {
                return null;
            }
            return text;
        }
        return null;
    }

    private List<Element> getLocations(String type, Document target) {
        Element locations = target.root().child("locations").orElse(null);
        if (locations != null) {
            return locations.children().filter(elem -> type.equals(elem.attribute("type")))
                    .toList();
        }
        return List.of();
    }

    @Override
    protected File getFileToBeUpdated() throws MojoFailureException {
        if (targetFile == null) {
            try {
                return TargetPlatformArtifactResolver.getMainTargetFile(getProject());
            } catch (TargetResolveException e) {
                throw new MojoFailureException(e);
            }
        } else {
            return targetFile;
        }
    }

    boolean isAllowSubIncrementalUpdates() {
        return allowSubIncrementalUpdates;
    }

    boolean isAllowIncrementalUpdates() {
        return allowIncrementalUpdates;
    }

    boolean isAllowMinorUpdates() {
        return allowMinorUpdates;
    }

    boolean isAllowMajorUpdates() {
        return allowMajorUpdates;
    }

    boolean isUpdateEmptyVersion() {
        return updateEmptyVersion;
    }

    MavenSession getMavenSession() {
        return mavenSession;
    }

    MojoExecution getMojoExecution() {
        return mojoExecution;
    }

    List<String> getUpdateSiteDiscovery() {
        if (updateSiteDiscovery == null) {
            return List.of();
        }
        return updateSiteDiscovery.stream().map(String::trim).toList();
    }

    Set<String> getMavenIgnoredVersions() {
        return mavenIgnoredVersions;
    }

    RuleSet getMavenRuleSet() {
        return mavenRuleSet;
    }

    String getMavenRulesUri() {
        if (mavenRulesUri != null && !mavenRulesUri.isBlank()) {
            try {
                URI u = new URI(mavenRulesUri);
                if (u.isAbsolute()) {
                    return mavenRulesUri;
                }
            } catch (URISyntaxException e) {
            }
            File fullPath = new File(mavenRulesUri);
            if (fullPath.isFile()) {
                return fullPath.toURI().toString();
            } else {
                File file = new File(getProject().getBasedir(), mavenRulesUri);
                if (file.exists()) {
                    return file.toURI().toString();
                }
            }
        }
        return mavenRulesUri;
    }

    Stream<Segment> getSegments() {
        Builder<Segment> builder = Stream.builder();
        if (isAllowMajorUpdates()) {
            builder.accept(Segment.MAJOR);
        }
        if (isAllowMinorUpdates()) {
            builder.accept(Segment.MINOR);
        }
        if (isAllowIncrementalUpdates()) {
            builder.accept(Segment.INCREMENTAL);
        }
        if (isAllowSubIncrementalUpdates()) {
            builder.accept(Segment.SUBINCREMENTAL);
        }
        return builder.build();
    }

}
