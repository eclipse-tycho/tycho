/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #658 - Tycho strips p2 artifact properties (eg PGP, maven info...)
 *    Marco Lehmann-Mörz - issue #2877 - tycho-versions-plugin:bump-versions does not honor SNAPSHOT suffix
 *******************************************************************************/
package org.eclipse.tycho;

import java.util.regex.Pattern;

public interface TychoConstants {

    public static final String ECLIPSE_LATEST = "https://download.eclipse.org/releases/2023-09/";

    public static final String TYCHO_NOT_CONFIGURED = "Tycho build extension not configured for ";

    static final String ANY_QUALIFIER = "qualifier";

    static final boolean USE_SMART_BUILDER = Boolean
            .parseBoolean(System.getProperty("tycho.build.smartbuilder", "true"));

    static final String SESSION_PROPERTY_TYCHO_MODE = "tycho.mode";
    static final String SESSION_PROPERTY_TYCHO_BUILDER = "tycho.builder";

    static final String P2_GROUPID_PREFIX = "p2.";

    static final String CONFIG_INI_PATH = "configuration/config.ini";
    static final String BUNDLES_INFO_PATH = "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
    static final String PLATFORM_XML_PATH = "configuration/org.eclipse.update/platform.xml";

    static final String CTX_BASENAME = TychoConstants.class.getName();

    // static final String CTX_TARGET_PLATFORM -> moved to TargetPlatform.FINAL_TARGET_PLATFORM_KEY;
    static final String CTX_DEPENDENCY_ARTIFACTS = CTX_BASENAME + "/dependencyArtifacts";
    static final String CTX_METADATA_ARTIFACT_LOCATION = CTX_BASENAME + "/metadataArtifactLocation";

    /**
     * Stores test-specific dependencies (usually derived from .classpath)
     */
    static final String CTX_TEST_DEPENDENCY_ARTIFACTS = CTX_BASENAME + "/testDependencyArtifacts";
    static final String CTX_ECLIPSE_PLUGIN_TEST_CLASSPATH = CTX_BASENAME + "/eclipsePluginTestClasspath";
    static final String CTX_ECLIPSE_PLUGIN_TEST_EXTRA_CLASSPATH = CTX_BASENAME + "/eclipsePluginTestClasspathExtra";

    static final String CTX_EXECUTION_ENVIRONMENT_CONFIGURATION = CTX_BASENAME + "/executionEnvironmentConfiguration";

    static final String CTX_DEPENDENCY_WALKER = CTX_BASENAME + "/dependencyWalker";
    static final String CTX_DEPENDENCY_SEEDS = CTX_BASENAME + "/dependencySeeds";

    static final Pattern PLATFORM_URL_PATTERN = Pattern.compile("platform:/(plugin|fragment)/([^/]*)(/)*.*");

    static final String PDE_BND = "pde.bnd";

    public String JAR_EXTENSION = "jar";

    String PROP_GROUP_ID = "maven-groupId";

    String PROP_ARTIFACT_ID = "maven-artifactId";

    String PROP_VERSION = "maven-version";

    String PROP_CLASSIFIER = "maven-classifier";

    String PROP_REPOSITORY = "maven-repository";

    String PROP_WRAPPED_GROUP_ID = "maven-wrapped-groupId";

    String PROP_WRAPPED_ARTIFACT_ID = "maven-wrapped-artifactId";

    String PROP_WRAPPED_VERSION = "maven-wrapped-version";

    String PROP_WRAPPED_CLASSIFIER = "maven-wrapped-classifier";

    String PROP_PGP_KEYS = "pgp.publicKeys";

    String PROP_PGP_SIGNATURES = "pgp.signatures";

    /**
     * @deprecated this is deprecated but can't be removed as we otherwise loose compatibility for
     *             older repository format, this should never be used in new code and usage should
     *             clearly be documented
     */
    @Deprecated(forRemoval = false)
    String PROP_EXTENSION = "maven-extension";
    String PROP_TYPE = "maven-type";

    String CLASSIFIER_P2_METADATA = "p2metadata";

    String EXTENSION_P2_METADATA = "xml";

    /**
     * Name of the file where the module p2 metadata is stored in the target directory. The name
     * needs to be known so that the target folder can be read as p2 metadata repository.
     */
    String FILE_NAME_P2_METADATA = "p2content.xml";

    String CLASSIFIER_P2_ARTIFACTS = "p2artifacts";

    String EXTENSION_P2_ARTIFACTS = "xml";

    /**
     * Name of the file that contains the p2 artifact index. This file is one of the files needed to
     * read the target folder as p2 artifact repository. The location is relative to the build
     * target directory root.
     */
    String FILE_NAME_P2_ARTIFACTS = "p2artifacts.xml";

    /**
     * Name of the file that stores the location of the Maven artifact in the target folder. This
     * file is one of the files needed to read the target folder as p2 artifact repository.
     */
    String FILE_NAME_LOCAL_ARTIFACTS = "local-artifacts.properties";

    /**
     * Key for the main artifact location in {@value FILE_NAME_LOCAL_ARTIFACTS} files.
     */
    String KEY_ARTIFACT_MAIN = "artifact.main";

    String KEY_BASELINE_REPLACE_ARTIFACT_MAIN = CTX_BASENAME + "/BaselineReplacedMain";

    /**
     * Key prefix for attached artifact locations in {@value FILE_NAME_LOCAL_ARTIFACTS} files.
     */
    String KEY_ARTIFACT_ATTACHED = "artifact.attached.";

    public String ROOTFILE_CLASSIFIER = "root";

    public String ROOTFILE_EXTENSION = "zip";

    String HEADER_TESTCASES = "Test-Cases";

    String SUFFIX_QUALIFIER = ".qualifier";

    String SUFFIX_SNAPSHOT = "-SNAPSHOT";
}
