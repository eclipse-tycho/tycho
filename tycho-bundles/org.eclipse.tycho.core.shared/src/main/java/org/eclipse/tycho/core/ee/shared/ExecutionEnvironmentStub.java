/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tycho.core.ee.shared;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ExecutionEnvironmentStub implements ExecutionEnvironment {

    private final String profileName;
    private final Set<String> systemPackages;
    private Properties properties;

    public ExecutionEnvironmentStub(String profileName, String... systemPackages) {
        this.profileName = profileName;
        this.systemPackages = new HashSet<>(Arrays.asList(systemPackages));
        this.properties = new Properties();
        properties.put("org.osgi.framework.system.capabilities",
                "osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0");
    }

    @Override
    public String getProfileName() {
        return this.profileName;
    }

    @Override
    public Set<String> getSystemPackages() {
        return this.systemPackages;
    }

    @Override
    public Properties getProfileProperties() {
        return this.properties;
    }

    @Override
    public String getCompilerSourceLevelDefault() {
        return "1";
    }

    @Override
    public String getCompilerTargetLevelDefault() {
        return "1";
    }

    @Override
    public boolean isCompatibleCompilerTargetLevel(String targetLevel) {
        return false;
    }

}
