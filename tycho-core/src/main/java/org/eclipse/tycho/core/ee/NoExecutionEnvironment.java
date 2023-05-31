/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.eclipse.tycho.ExecutionEnvironment;

public class NoExecutionEnvironment implements ExecutionEnvironment {

    private static final Properties EMPTY_PROPERTIES = new Properties();
    public static final String NAME = "none";
    public static final NoExecutionEnvironment INSTANCE = new NoExecutionEnvironment();

    private NoExecutionEnvironment() {
        // make privte for singleton
    }

    @Override
    public String getProfileName() {
        return NAME;
    }

    @Override
    public Collection<SystemPackageEntry> getSystemPackages() {
        return Collections.emptySet();
    }

    @Override
    public Properties getProfileProperties() {
        return EMPTY_PROPERTIES;
    }

    @Override
    public String getCompilerSourceLevelDefault() {
        throw new IllegalStateException("The 'none' execution environment shouldn't be dereferenced for compilation");
    }

    @Override
    public String getCompilerTargetLevelDefault() {
        throw new IllegalStateException("The 'none' execution environment shouldn't be dereferenced for compilation");
    }

    @Override
    public boolean isCompatibleCompilerTargetLevel(String targetLevel) {
        throw new IllegalStateException("The 'none' execution environment shouldn't be dereferenced for compilation");
    }

}
