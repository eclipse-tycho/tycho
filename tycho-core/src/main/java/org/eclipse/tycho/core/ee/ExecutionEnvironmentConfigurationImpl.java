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

public class ExecutionEnvironmentConfigurationImpl implements ExecutionEnvironmentConfiguration {
    private ExecutionEnvironment executionEnvironment;

    public void setFullSpecification(ExecutionEnvironment executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }

    public String getProfileName() {
        if (executionEnvironment == null) {
            // TODO 387796 return the global default instead
            return null;
        }
        return executionEnvironment.getProfileName();
    }

    public ExecutionEnvironment getFullSpecification() {
        return executionEnvironment;
    }
}
