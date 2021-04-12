/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.core.ee.shared;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ExecutionEnvironmentStub implements ExecutionEnvironment {

    private final String profileName;
    private final List<SystemPackageEntry> systemPackages;
    private Properties properties;

    public ExecutionEnvironmentStub(String profileName, String... systemPackages) {
        this.profileName = profileName;
        this.systemPackages = Arrays.stream(systemPackages).map(pack -> new SystemPackageEntry(pack, null))
                .collect(Collectors.toList());
        this.properties = new Properties();
        properties.put("org.osgi.framework.system.capabilities",
                "osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0");
    }

    @Override
    public String getProfileName() {
        return this.profileName;
    }

    @Override
    public Collection<SystemPackageEntry> getSystemPackages() {
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
