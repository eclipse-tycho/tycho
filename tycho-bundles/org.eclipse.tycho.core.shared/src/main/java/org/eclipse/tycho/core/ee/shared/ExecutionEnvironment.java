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

public interface ExecutionEnvironment {

    String getProfileName();

    String getCompilerSourceLevel();

    String getCompilerTargetLevel();

    String[] getSystemPackages();

    Properties getProfileProperties();

    /**
     * Returns <code>true</code> if classes compiled for the specified target can be executed in
     * this execution environment or if this environment's compiler target compatibility is unknown.
     */
    boolean isCompatibleCompilerTargetLevel(String target);

}
