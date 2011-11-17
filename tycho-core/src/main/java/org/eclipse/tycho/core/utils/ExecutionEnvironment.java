/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.utils;

import java.util.Properties;

/**
 * Represents an OSGi execution environment a.k.a. profile. Execution environments are referenced in
 * MANIFEST.MF using the header "Bundle-RequiredExecutionEnvironment".
 * 
 * See the list of known OSGi profiles in bundle org.eclipse.osgi, file profile.list.
 * 
 */
public class ExecutionEnvironment {

    private String profileName;
    private String compilerSourceLevel;
    private String compilerTargetLevel;
    private String[] systemPackages;

    /**
     * Do no instantiate. Use factory method instead
     * {@link ExecutionEnvironmentUtils#getExecutionEnvironment(String)}.
     */
    /* package */ExecutionEnvironment(Properties profileProperties) {
        this.profileName = profileProperties.getProperty("osgi.java.profile.name");
        this.compilerSourceLevel = profileProperties.getProperty("org.eclipse.jdt.core.compiler.source");
        this.compilerTargetLevel = profileProperties
                .getProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform");
        this.systemPackages = profileProperties.getProperty("org.osgi.framework.system.packages").split(",");
    }

    public String getProfileName() {
        return profileName;
    }

    public String getCompilerSourceLevel() {
        return compilerSourceLevel;
    }

    public String getCompilerTargetLevel() {
        return compilerTargetLevel;
    }

    /*
     * for debug purposes
     */
    @Override
    public String toString() {
        return "OSGi profile '" + getProfileName() + "' { source level: " + compilerSourceLevel + ", target level: "
                + compilerTargetLevel + "}";
    }

    public String[] getSystemPackages() {
        return systemPackages;
    }

}
