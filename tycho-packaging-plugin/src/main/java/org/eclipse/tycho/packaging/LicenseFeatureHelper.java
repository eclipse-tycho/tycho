/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesImpl;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Feature;

@Component(role = LicenseFeatureHelper.class)
public class LicenseFeatureHelper {

    public LicenseFeatureHelper() {
    }

    /**
     * Get the license feature jar for feature (or <code>null</code> if it has no license feature).
     * 
     * See {@linkplain http://wiki.eclipse.org/Equinox/p2/License_Mechanism }.
     * 
     * @param feature
     *            original feature
     * @param mavenProject
     *            original feature project
     * @return the license feature jar
     */
    public File getLicenseFeature(Feature feature, MavenProject mavenProject) {
        String id = feature.getLicenseFeature();

        if (id == null) {
            return null;
        }

        ArtifactDescriptor licenseFeature = TychoProjectUtils.getDependencyArtifacts(mavenProject).getArtifact(
                ArtifactKey.TYPE_ECLIPSE_FEATURE, id, feature.getLicenseFeatureVersion());

        if (licenseFeature == null) {
            throw new IllegalStateException("License feature with id " + id
                    + " is not found among project dependencies");
        }

        ReactorProject licenseProject = licenseFeature.getMavenProject();
        if (licenseProject == null) {
            return licenseFeature.getLocation();
        }

        File artifact = licenseProject.getArtifact();
        if (!artifact.isFile()) {
            throw new IllegalStateException("At least ''package'' phase need to be executed");
        }

        return artifact;
    }

    /**
     * Get all files included in license feature jar (via build.properties
     * bin.includes/bin.excludes) exept for feature.xml, feature.properties and build.properties as
     * an archived fileset so they can be added to another feature jar.
     * 
     * @param licenseFeature
     *            license feature jar
     */
    public ArchivedFileSet getLicenseFeatureFileSet(File licenseFeature) throws IOException {
        // copy all files from license feature's build.properties file except 
        // feature.properties, feature.xml and build.properties itself
        BuildProperties buildProperties;

        ZipFile zip = new ZipFile(licenseFeature);
        try {
            ZipEntry entry = zip.getEntry(BuildPropertiesParser.BUILD_PROPERTIES);
            if (entry != null) {
                InputStream is = zip.getInputStream(entry);
                Properties p = new Properties();
                p.load(is);
                buildProperties = new BuildPropertiesImpl(p);
            } else {
                throw new IllegalArgumentException("license feature must include build.properties file");
            }
        } finally {
            zip.close();
        }

        List<String> includes = buildProperties.getBinIncludes();

        Set<String> excludes = new HashSet<String>(buildProperties.getBinExcludes());
        excludes.add(Feature.FEATURE_XML);
        excludes.add("feature.properties");
        excludes.add(BuildPropertiesParser.BUILD_PROPERTIES);

        // mavenArchiver ignores license feature files that are also present in 'this' feature
        // i.e. if there is a conflict, files from 'this' feature win

        DefaultArchivedFileSet result = new DefaultArchivedFileSet();
        result.setArchive(licenseFeature);
        result.setIncludes(includes.toArray(new String[includes.size()]));
        result.setExcludes(excludes.toArray(new String[excludes.size()]));

        return result;
    }

}
