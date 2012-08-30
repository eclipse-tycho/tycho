/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee.test.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;

// TODO do we need better defaults for the other attributes?
public class ExecutionEnvironmentStub implements ExecutionEnvironment {

    private Properties properties = new Properties();
    private List<String> systemPackages = new ArrayList<String>();

    public String getProfileName() {
        return null;
    }

    public String getCompilerSourceLevel() {
        return null;
    }

    public String getCompilerTargetLevel() {
        return null;
    }

    public String[] getSystemPackages() {
        return systemPackages.toArray(new String[0]);
    }

    public void addSystemPackage(String systemPackage) {
        this.systemPackages.add(systemPackage);
    }

    public Properties getProfileProperties() {
        return properties;
    }

    public void setProfileProperty(String key, String value) {
        this.properties.put(key, value);
    }

    public boolean isCompatibleCompilerTargetLevel(String target) {
        return false;
    }

}
