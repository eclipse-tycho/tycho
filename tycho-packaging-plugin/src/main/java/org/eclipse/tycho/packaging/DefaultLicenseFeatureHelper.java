/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.BuildPropertiesImpl;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.Feature;

@Named
@Singleton
public class DefaultLicenseFeatureHelper implements LicenseFeatureHelper {

	@Inject
	TychoProjectManager projectManager;

    public DefaultLicenseFeatureHelper() {
    }

    /**
     * Get the license feature jar for feature (or <code>null</code> if it has no license feature).
     * 
     * See {@linkplain https://wiki.eclipse.org/Equinox/p2/License_Mechanism }.
     * 
     * @param feature
     *            original feature
     * @param mavenProject
     *            original feature project
     * @return the license feature jar
     */
	@Override
    public File getLicenseFeature(Feature feature, MavenProject mavenProject) {
        String id = feature.getLicenseFeature();

        if (id == null) {
            return null;
        }
		Optional<TychoProject> tychoProject = projectManager.getTychoProject(mavenProject);
		if (tychoProject.isEmpty()) {
			return null;
		}

		ArtifactDescriptor licenseFeature = tychoProject.get()
                .getDependencyArtifacts(DefaultReactorProject.adapt(mavenProject))
                .getArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE, id, feature.getLicenseFeatureVersion());

        if (licenseFeature == null) {
            throw new IllegalStateException(
                    "License feature with id " + id + " is not found among project dependencies");
        }

        ReactorProject licenseProject = licenseFeature.getMavenProject();
        if (licenseProject == null) {
            return licenseFeature.getLocation(true);
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
	@Override
    public ArchivedFileSet getLicenseFeatureFileSet(File licenseFeature) throws IOException {
        // copy all files from license feature's build.properties file except 
        // feature.properties, feature.xml and build.properties itself
        BuildProperties buildProperties;

        try (ZipFile zip = new ZipFile(licenseFeature)) {
            ZipEntry entry = zip.getEntry(BuildPropertiesParser.BUILD_PROPERTIES);
            if (entry != null) {
                InputStream is = zip.getInputStream(entry);
                Properties p = new Properties();
                p.load(is);
                buildProperties = new BuildPropertiesImpl(p);
            } else {
                throw new IllegalArgumentException("license feature must include build.properties file");
            }
        }

        List<String> includes = buildProperties.getBinIncludes();

        Set<String> excludes = new HashSet<>(buildProperties.getBinExcludes());
        excludes.add(Feature.FEATURE_XML);
        excludes.add("feature.properties");
        excludes.add(BuildPropertiesParser.BUILD_PROPERTIES);

        // mavenArchiver ignores license feature files that are also present in 'this' feature
        // i.e. if there is a conflict, files from 'this' feature win

        DefaultArchivedFileSet result = new DefaultArchivedFileSet(licenseFeature);
        result.setIncludes(includes.toArray(new String[includes.size()]));
        result.setExcludes(excludes.toArray(new String[excludes.size()]));

        return result;
    }

}
