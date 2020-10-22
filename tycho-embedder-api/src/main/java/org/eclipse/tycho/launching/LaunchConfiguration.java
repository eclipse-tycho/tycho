/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.launching;

import java.io.File;
import java.util.Map;

public interface LaunchConfiguration {
    public Map<String, String> getEnvironment();

    public String getJvmExecutable();

    public File getWorkingDirectory();

    public String[] getProgramArguments();

    public String[] getVMArguments();

    public File getLauncherJar();
}
