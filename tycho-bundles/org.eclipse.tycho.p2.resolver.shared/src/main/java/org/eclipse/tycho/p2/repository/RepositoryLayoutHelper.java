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
import java.util.StringTokenizer;

import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.PackagingType;

public class RepositoryLayoutHelper {
    public static final String PROP_GROUP_ID = "maven-groupId";

    public static final String PROP_ARTIFACT_ID = "maven-artifactId";

    public static final String PROP_VERSION = "maven-version";

    public static final String PROP_CLASSIFIER = "maven-classifier";

    public static final String PROP_REPOSITORY = "maven-repository";

    public static final String PROP_EXTENSION = "maven-extension";

    public static final String CLASSIFIER_P2_METADATA = "p2metadata";

    public static final String EXTENSION_P2_METADATA = "xml";

    /**
     * Name of the file where the module p2 metadata is stored in the target directory. The name
     * needs to be known so that the target folder can be read as p2 metadata repository.
     */
    public static final String FILE_NAME_P2_METADATA = "p2content.xml";

    public static final String CLASSIFIER_P2_ARTIFACTS = "p2artifacts";

    public static final String EXTENSION_P2_ARTIFACTS = "xml";

    /**
     * Name of the file that contains the p2 artifact index. This file is one of the files needed to
     * read the target folder as p2 artifact repository. The location is relative to the build
     * target directory root.
     */
    public static final String FILE_NAME_P2_ARTIFACTS = "p2artifacts.xml";

    /**
     * Name of the file that stores the location of the Maven artifact in the target folder. This
     * file is one of the files needed to read the target folder as p2 artifact repository.
     */
    public static final String FILE_NAME_LOCAL_ARTIFACTS = "local-artifacts.properties";

    /**
     * Key for the main artifact location in {@value #FILE_NAME_LOCAL_ARTIFACTS} files.
     */
    public static final String KEY_ARTIFACT_MAIN = "artifact.main";

    /**
     * Key prefix for attached artifact locations in {@value #FILE_NAME_LOCAL_ARTIFACTS} files.
     */
    public static final String KEY_ARTIFACT_ATTACHED = "artifact.attached.";

    public static final String DEFAULT_EXTERNSION = "jar";

    public static final String PACK200_CLASSIFIER = "pack200";
    public static final String PACK200_EXTENSION = "jar.pack.gz";

    public static String getRelativePath(GAV gav, String classifier, String extension) {
        return getRelativePath(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), classifier, extension);
    }

    public static String getRelativePath(String groupId, String artifactId, String version, String classifier,
            String extension) {
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
        if (extension != null) {
            //Workaround for #593
            switch (extension) {
            // dup of ArtifactType.TYPE_ECLIPSE_PLUGIN case PackagingType.TYPE_ECLIPSE_PLUGIN:
            case ArtifactType.TYPE_ECLIPSE_PLUGIN:
            case ArtifactType.TYPE_ECLIPSE_TEST_FRAGMENT:
            // dup of ArtifactType.TYPE_ECLIPSE_FEATURE case PackagingType.TYPE_ECLIPSE_FEATURE:
            case ArtifactType.TYPE_ECLIPSE_FEATURE:
            case PackagingType.TYPE_ECLIPSE_TEST_PLUGIN:
            case "ejb":
            case "ejb-client":
            case "test-jar":
            case "javadoc":
            case "java-source":
            case "maven-plugin":
                extension = "jar";
                break;
            case PackagingType.TYPE_ECLIPSE_UPDATE_SITE:
            case PackagingType.TYPE_ECLIPSE_REPOSITORY:
            case PackagingType.TYPE_ECLIPSE_APPLICATION:
            case ArtifactType.TYPE_ECLIPSE_PRODUCT:
                extension = "zip";
                break;
            // dup of ArtifactType.TYPE_INSTALLABLE_UNIT case PackagingType.TYPE_P2_IU:
            case ArtifactType.TYPE_INSTALLABLE_UNIT:
                extension = "xml";
                break;
            case PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION:
                extension = "target";
                break;
            default:
                break;
            }
        }

        sb.append('.').append(extension != null ? extension : DEFAULT_EXTERNSION);

        return sb.toString();
    }

    public static GAV getP2Gav(String classifier, String id, String version) {
        // Should match MavenDependencyCollector#createSystemScopeDependency
        return new GAV("p2." + classifier, id, version);
    }

    // TODO these methods do not belong here - they should go to GAV or some kind of GAV helper
    // TODO writing to Maps should be implemented next to reading

    public static GAV getGAV(Map<?, ?> properties) {
        String groupId = (String) properties.get(PROP_GROUP_ID);
        String artifactId = (String) properties.get(PROP_ARTIFACT_ID);
        String version = (String) properties.get(PROP_VERSION);

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
        return (String) properties.get(PROP_CLASSIFIER);
    }

    public static String getExtension(Map<?, ?> properties) {
        if (properties == null) {
            return null;
        }
        String explicitExtension = (String) properties.get(PROP_EXTENSION);
        return explicitExtension == null ? DEFAULT_EXTERNSION : explicitExtension;
    }
}
