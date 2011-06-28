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
