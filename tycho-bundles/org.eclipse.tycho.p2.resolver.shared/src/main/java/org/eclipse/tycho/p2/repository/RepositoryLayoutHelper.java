/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 519221 - The Maven artifact to be added to the target platform is not stored at the required location on disk
 *******************************************************************************/
package org.eclipse.tycho.p2.repository;

import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.MavenContext;

public class RepositoryLayoutHelper {
    public static String getRelativePath(GAV gav, String classifier, String type, MavenContext mavenContext) {
        return getRelativePath(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), classifier, type, mavenContext);
    }

    public static String getRelativePath(GAV gav, String classifier, String extension) {
        if (extension == null) {
            extension = TychoConstants.JAR_EXTENSION;
        }
        return getRelativePath(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), classifier, extension);
    }

    public static String getRelativePath(String groupId, String artifactId, String version, String classifier,
            String type, MavenContext mavenContext) {
        //we need to handle some legacy cases here where type == extension was used
        String extension;
        if (type == null) {
            extension = TychoConstants.JAR_EXTENSION;
        } else if (TychoConstants.CLASSIFIER_P2_METADATA.equals(classifier)) {
            extension = TychoConstants.EXTENSION_P2_METADATA;
        } else if (TychoConstants.CLASSIFIER_P2_ARTIFACTS.equals(classifier)) {
            extension = TychoConstants.EXTENSION_P2_ARTIFACTS;
        } else if (TychoConstants.ROOTFILE_CLASSIFIER.equals(classifier)
                || (classifier != null && classifier.startsWith(TychoConstants.ROOTFILE_CLASSIFIER + "."))) {
            extension = TychoConstants.ROOTFILE_EXTENSION;
        } else {
            switch (type) {
            case TychoConstants.JAR_EXTENSION:
            case "zip":
            case "target":
            case "xml":
                extension = type;
                break;
            default:
                extension = mavenContext.getExtension(type);
            }
        }
        return getRelativePath(groupId, artifactId, version, classifier, extension);
    }

    public static String getRelativePath(String groupId, String artifactId, String version, String classifier,
            String extension) {
        Objects.requireNonNull(extension);

        StringBuilder sb = new StringBuilder();

        // basedir
        StringTokenizer st = new StringTokenizer(groupId, ".");
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken()).append('/');
        }
        sb.append(artifactId).append('/').append(version).append('/');

        // filename
        sb.append(artifactId).append('-').append(version);
        if (classifier != null && !classifier.isEmpty()) {
            sb.append('-').append(classifier);
        }

        sb.append('.').append(extension);

        return sb.toString();
    }

    public static GAV getP2Gav(String classifier, String id, String version) {
        return new GAV(TychoConstants.P2_GROUPID_PREFIX + classifier, id, version);
    }

    // TODO these methods do not belong here - they should go to GAV or some kind of GAV helper
    // TODO writing to Maps should be implemented next to reading

    public static GAV getGAV(Map<?, ?> properties) {
        String groupId = (String) properties.get(TychoConstants.PROP_GROUP_ID);
        String artifactId = (String) properties.get(TychoConstants.PROP_ARTIFACT_ID);
        String version = (String) properties.get(TychoConstants.PROP_VERSION);

        // TODO partial information should be an error!?
        return getGAV(groupId, artifactId, version);
    }

    public static GAV getGAV(String groupId, String artifactId, String version) {
        if (groupId != null && artifactId != null && version != null) {
            return new GAV(groupId, artifactId, version);
        }

        return null;
    }

    // TODO it would be useful to have a GAV+C+T class
    public static String getClassifier(Map<?, ?> properties) {
        if (properties == null) {
            return null;
        }
        return (String) properties.get(TychoConstants.PROP_CLASSIFIER);
    }

    public static String getType(Map<?, ?> properties) {
        if (properties == null) {
            return null;
        }
        String type = (String) properties.get(TychoConstants.PROP_TYPE);
        if (type == null) {
            //fallback for older repository formats
            type = (String) properties.get(TychoConstants.PROP_EXTENSION);
        }
        return type;
    }
}
