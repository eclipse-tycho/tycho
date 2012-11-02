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

import java.util.Properties;
import java.util.Set;

public interface ExecutionEnvironment {

    String getProfileName();

    /**
     * Returns the list of packages (without versions) provided by the execution environment.
     */
    Set<String> getSystemPackages();

    Properties getProfileProperties();

    /**
     * Returns a reasonable compiler source level default for this execution environment.
     * 
     * @return a compiler source level matching the execution environment, or <code>null</code> if
     *         unknown.
     */
    String getCompilerSourceLevelDefault();

    /**
     * Returns a reasonable compiler target level default for this execution environment.
     * 
     * @return a compiler target level matching the execution environment, or <code>null</code> if
     *         unknown.
     */
    String getCompilerTargetLevelDefault();

    /**
     * Returns <code>false</code> if classes compiled with the given compiler target level can
     * certainly not be executed on this execution environment. Used to detect inconsistent
     * configuration.
     */
    boolean isCompatibleCompilerTargetLevel(String targetLevel);

}
