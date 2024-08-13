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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.equinox.p2.core.ProvisionException;
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
     * If specified also update to new major versions of the dependency otherwise only perform
     * minor, micro or "qualifier" changes, please note that for maven locations the semantic might
     * be slightly different as maven does not follow OSGi version scheme, in this case we interpret
     * the first part of the version as the major version.
     */
    @Parameter(property = "major", defaultValue = "true")
    private boolean updateMajorVersion;

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

    @Component
    private MavenSession mavenSession;

    @Inject
    private MavenLocationUpdater mavenLocationUpdater;

    @Inject
    private InstallableUnitLocationUpdater installableUnitLocationUpdater;

    @Override
    protected void doUpdate() throws IOException, URISyntaxException, ParserConfigurationException,
            TargetResolveException, MojoFailureException, VersionRangeResolutionException, ArtifactResolutionException,
            InvalidVersionSpecificationException, ProvisionException {
        File file = getFileToBeUpdated();
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
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), enc); XMLWriter xw = new XMLWriter(w)) {
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

    boolean isUpdateMajorVersion() {
        return updateMajorVersion;
    }

    MavenSession getMavenSession() {
        return mavenSession;
    }

    String getUpdateSiteDiscovery() {
        return updateSiteDiscovery;
    }

}
