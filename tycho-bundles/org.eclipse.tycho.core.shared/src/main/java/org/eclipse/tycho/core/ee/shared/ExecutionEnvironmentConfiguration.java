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

/**
 * Instances of this type collect information on the configured execution environment, so that they
 * are eventually able to compute the full specification of the effective configuration.
 */
public interface ExecutionEnvironmentConfiguration {

    /**
     * Sets the effective profile configuration.
     */
    public void overrideProfileConfiguration(String profileName, String configurationOrigin);

    /**
     * Sets the effective profile configuration, unless the method
     * {@link #overrideProfileConfiguration(String, String)} has been called on this instance.
     */
    public void setProfileConfiguration(String profileName, String configurationOrigin);

    public String getProfileName();

    public ExecutionEnvironment getFullSpecification();

}
