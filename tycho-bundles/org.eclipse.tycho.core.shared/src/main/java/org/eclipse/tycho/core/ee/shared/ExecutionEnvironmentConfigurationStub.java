/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee.shared;

import java.util.List;

/**
 * {@link ExecutionEnvironmentConfiguration} instance usable for additional calls of the target
 * platform computation and dependency resolution.
 */
public class ExecutionEnvironmentConfigurationStub implements ExecutionEnvironmentConfiguration {

    private String profileName;

    /**
     * Creates a new {@link ExecutionEnvironmentConfiguration} for a standalone call of the target
     * platform computation.
     * 
     * @param profileName
     *            a standard execution environment
     */
    public ExecutionEnvironmentConfigurationStub(String profileName) {
        if (profileName == null) {
            throw new NullPointerException();
        }
        this.profileName = profileName;
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    @Override
    public boolean isCustomProfile() {
        return false;
    }

    @Override
    public void setProfileConfiguration(String profileName, String configurationOrigin) {
        // not needed
        throw new UnsupportedOperationException();
    }

    @Override
    public void overrideProfileConfiguration(String profileName, String configurationOrigin) {
        // not needed
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFullSpecificationForCustomProfile(List<SystemCapability> systemCapabilities) {
        // not needed
        throw new UnsupportedOperationException();
    }

    @Override
    public ExecutionEnvironment getFullSpecification() {
        // not needed
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIgnoredByResolver() {
        return false;
    }

}
