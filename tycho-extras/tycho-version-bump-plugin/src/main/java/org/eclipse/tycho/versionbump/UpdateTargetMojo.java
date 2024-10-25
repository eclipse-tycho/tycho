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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.eclipse.tycho.targetplatform.TargetPlatformArtifactResolver;
import org.eclipse.tycho.targetplatform.TargetResolveException;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

/**
 * This allows to update a target file to use newer version of specified items, e.g. IUs from
 * updatesites or maven coordinates. In the simplest form this is called like
 *
 * <pre>
 * mvn -f [path to target project] tycho-version-bump:update-target
 * </pre>
 * <p>
 * For updating <b>maven target locations</b> the mojo support
 * <a href="https://www.mojohaus.org/versions/versions-maven-plugin/version-rules.html">Version
 * number comparison rule-sets</a> similar to the
 * <a href="https://www.mojohaus.org/versions/versions-maven-plugin">Versions Maven Plugin</a>
 * please check the documentation there for further information about ruleset files.
 * </p>
 * <p>
 * For updating <b>installable unit locations</b> (also known as update sites)
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
     * A comma separated list of update site discovery strategies, the following is currently
     * supported:
     * <ul>
     * <li>parent - search the parent path for a composite that can be used to find newer
     * versions</li>
     * </ul>
     */
    @Parameter(property = "discovery")
    private String updateSiteDiscovery;

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

    @Component
    private MavenSession mavenSession;

    @Parameter(defaultValue = "${mojoExecution}", required = true, readonly = true)
    private MojoExecution mojoExecution;

    @Inject
    private MavenLocationUpdater mavenLocationUpdater;

    @Inject
    private InstallableUnitLocationUpdater installableUnitLocationUpdater;

    @Override
    protected void doUpdate(File file) throws Exception {
        getLog().info("Update target file " + file);
        //we use the descent xml parser here because we need to retain the formating of the original file
        XMLParser parser = new XMLParser();
        Document target = parser.parse(new XMLIOSource(file));
        boolean changed = false;
        try (FileInputStream input = new FileInputStream(file)) {
            for (Element iuLocation : getLocations("InstallableUnit", target)) {
                changed |= installableUnitLocationUpdater.update(iuLocation, this);
            }
            for (Element mavenLocation : getLocations("Maven", target)) {
                changed |= mavenLocationUpdater.update(mavenLocation, this);
            }
        }
        if (changed) {
            String enc = target.getEncoding() != null ? target.getEncoding() : "UTF-8";
            try (Writer w = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), enc);
                    XMLWriter xw = new XMLWriter(w)) {
                try {
                    target.toXML(xw);
                } finally {
                    xw.flush();
                }
            }
        }
    }

    static void setElementValue(String name, String value, Element root) {
        Element child = root.getChild(name);
        if (child != null) {
            child.setText(value);
        }
    }

    static String getElementValue(String name, Element root) {
        Element child = root.getChild(name);
        if (child != null) {
            String text = child.getText().trim();
            if (text.isBlank()) {
                return null;
            }
            return text;
        }
        return null;
    }

    private List<Element> getLocations(String type, Document target) {
        Element locations = target.getRootElement().getChild("locations");
        if (locations != null) {
            return locations.getChildren().stream().filter(elem -> type.equals(elem.getAttributeValue("type")))
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

    MavenSession getMavenSession() {
        return mavenSession;
    }

    MojoExecution getMojoExecution() {
        return mojoExecution;
    }

    String getUpdateSiteDiscovery() {
        return updateSiteDiscovery;
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
