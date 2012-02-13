/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

public interface TychoConstants {
    static final String CONFIG_INI_PATH = "configuration/config.ini";
    static final String BUNDLES_INFO_PATH = "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
    static final String PLATFORM_XML_PATH = "configuration/org.eclipse.update/platform.xml";

    static final String CTX_BASENAME = TychoConstants.class.getName();
    static final String CTX_TARGET_PLATFORM = CTX_BASENAME + "/targetPlatform";
    static final String CTX_DEPENDENCY_ARTIFACTS = CTX_BASENAME + "/dependencyArtifacts";
    static final String CTX_ECLIPSE_PLUGIN_PROJECT = CTX_BASENAME + "/eclipsePluginProject";
    static final String CTX_ECLIPSE_PLUGIN_CLASSPATH = CTX_BASENAME + "/eclipsePluginClasspath";
    static final String CTX_ECLIPSE_PLUGIN_BOOTCLASSPATH_EXTRA_ACCESSRULES = CTX_BASENAME
            + "/eclipsePluginBootclasspathExtraAccessRules";
    static final String CTX_EXPANDED_VERSION = CTX_BASENAME + "/expandedVersion";
    static final String CTX_MERGED_PROPERTIES = CTX_BASENAME + "/mergedProperties";
    static final String CTX_TARGET_PLATFORM_CONFIGURATION = CTX_BASENAME + "/targetPlatformConfiguration";
    static final String CTX_DEPENDENCY_WALKER = CTX_BASENAME + "/dependencyWalker";
    static final String CTX_PUBLISHED_ROOT_IUS = CTX_BASENAME + "/publishedRootIUs";
}
