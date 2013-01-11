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

    public String getProfileName() {
        return profileName;
    }

    public boolean isCustomProfile() {
        return false;
    }

    public void setProfileConfiguration(String profileName, String configurationOrigin) {
        // not needed
        throw new UnsupportedOperationException();
    }

    public void overrideProfileConfiguration(String profileName, String configurationOrigin) {
        // not needed
        throw new UnsupportedOperationException();
    }

    public void setFullSpecificationForCustomProfile(List<SystemCapability> systemCapabilities) {
        // not needed
        throw new UnsupportedOperationException();
    }

    public ExecutionEnvironment getFullSpecification() {
        // not needed
        throw new UnsupportedOperationException();
    }

}
