/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #658 - Tycho strips p2 artifact properties (eg PGP, maven info...)
 *******************************************************************************/
package org.eclipse.tycho;

public interface TychoConstants {

    static final String P2_GROUPID_PREFIX = "p2.";

    static final String CONFIG_INI_PATH = "configuration/config.ini";
    static final String BUNDLES_INFO_PATH = "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
    static final String PLATFORM_XML_PATH = "configuration/org.eclipse.update/platform.xml";

    static final String CTX_BASENAME = TychoConstants.class.getName();

    // static final String CTX_TARGET_PLATFORM -> moved to TargetPlatform.FINAL_TARGET_PLATFORM_KEY;
    static final String CTX_DEPENDENCY_ARTIFACTS = CTX_BASENAME + "/dependencyArtifacts";
    /**
     * Stores test-specific dependencies (usually derived from .classpath)
     */
    static final String CTX_TEST_DEPENDENCY_ARTIFACTS = CTX_BASENAME + "/testDependencyArtifacts";
    static final String CTX_ECLIPSE_PLUGIN_PROJECT = CTX_BASENAME + "/eclipsePluginProject";
    static final String CTX_ECLIPSE_PLUGIN_TEST_CLASSPATH = CTX_BASENAME + "/eclipsePluginTestClasspath";
    static final String CTX_ECLIPSE_PLUGIN_TEST_EXTRA_CLASSPATH = CTX_BASENAME + "/eclipsePluginTestClasspathExtra";

    static final String CTX_MERGED_PROPERTIES = CTX_BASENAME + "/mergedProperties";
    static final String CTX_TARGET_PLATFORM_CONFIGURATION = CTX_BASENAME + "/targetPlatformConfiguration";
    static final String CTX_EXECUTION_ENVIRONMENT_CONFIGURATION = CTX_BASENAME + "/executionEnvironmentConfiguration";

    static final String CTX_DEPENDENCY_WALKER = CTX_BASENAME + "/dependencyWalker";
    static final String CTX_DEPENDENCY_SEEDS = CTX_BASENAME + "/dependencySeeds";

    public String JAR_EXTENSION = "jar";

    String PROP_GROUP_ID = "maven-groupId";

    String PROP_ARTIFACT_ID = "maven-artifactId";

    String PROP_VERSION = "maven-version";

    String PROP_CLASSIFIER = "maven-classifier";

    String PROP_REPOSITORY = "maven-repository";

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

    /**
     * Key prefix for attached artifact locations in {@value FILE_NAME_LOCAL_ARTIFACTS} files.
     */
    String KEY_ARTIFACT_ATTACHED = "artifact.attached.";

    public String ROOTFILE_CLASSIFIER = "root";

    public String ROOTFILE_EXTENSION = "zip";
}
