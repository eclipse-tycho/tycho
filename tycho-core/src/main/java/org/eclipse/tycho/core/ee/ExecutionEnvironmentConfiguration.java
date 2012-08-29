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
package org.eclipse.tycho.core.ee;

/**
 * Instances of this type collect information on the configured execution environment, so that they
 * are eventually able to compute the full specification of the effective configuration.
 */
public interface ExecutionEnvironmentConfiguration {

    public void setFullSpecification(ExecutionEnvironment executionEnvironment);

    public String getProfileName();

    public ExecutionEnvironment getFullSpecification();

}
