/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.equinox.launching;

import org.eclipse.tycho.launching.LaunchConfiguration;

public interface EquinoxLauncher {
    public int execute(LaunchConfiguration configuration, int forkedProcessTimeoutInSeconds)
            throws EquinoxLaunchingException;
}
