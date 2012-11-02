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
 * <p>
 * Instances of this type collect information on the execution environment a project is targeted to.
 * Since there are multiple ways to configure the execution environment, and some of them are
 * project type specific, it is explicitly allowed to call the configuration setters of an instance
 * multiple times. In order to ensure that all configuration sources are taken into account,
 * {@link IllegalStateException}s are thrown if the configuration setters are called too late, i.e.
 * after the effective configuration has been queried for the first time.
 * </p>
 * <p>
 * In case of an custom execution environment, the information required for the build can not be
 * directly computed from the configuration. Therefore, this instance also has a callback to add the
 * missing information as soon as it has been extracted from the target platform.
 * <p>
 */
public interface ExecutionEnvironmentConfiguration {

    /**
     * Sets the effective profile configuration.
     * 
     * @throws IllegalStateException
     *             if the configuration has been already frozen by calling any one of the getters
     *             defined in {@link ExecutionEnvironmentConfiguration}
     */
    public void overrideProfileConfiguration(String profileName, String configurationOrigin)
            throws IllegalStateException;

    /**
     * Sets the effective profile configuration, unless the method
     * {@link #overrideProfileConfiguration(String, String)} has been called on this instance.
     * 
     * @throws IllegalStateException
     *             if the configuration has been already frozen by calling any one of the getters
     *             defined in {@link ExecutionEnvironmentConfiguration}
     */
    public void setProfileConfiguration(String profileName, String configurationOrigin) throws IllegalStateException;

    /**
     * Returns the name of the configured profile.
     */
    public String getProfileName();

    /**
     * Returns <code>true</code> if the configured profile is not one of the known standard
     * execution environments.
     */
    public boolean isCustomProfile();

    /**
     * Call-back for setting the actual specification for the configured custom profile. The
     * specification, e.g. the list of provided packages, is read from the target platform.
     * 
     * @throws IllegalStateException
     *             if the configured execution environment profile is not a custom profile
     * @see #isCustomProfile()
     */
    public void setFullSpecificationForCustomProfile(List<SystemCapability> systemCapabilities)
            throws IllegalStateException;

    /**
     * Returns the execution environment specification with information needed for the build.
     * 
     * @throws IllegalStateException
     *             if a custom execution enviromnent profile has been configure, and
     *             {@link #setFullSpecificationForCustomProfile(List)} has not been called.
     * @see ExecutionEnvironment
     */
    public ExecutionEnvironment getFullSpecification() throws IllegalStateException;

}
