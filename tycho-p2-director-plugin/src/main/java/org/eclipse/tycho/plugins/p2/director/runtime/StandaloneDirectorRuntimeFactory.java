/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director.runtime;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

public interface StandaloneDirectorRuntimeFactory {

    StandaloneDirectorRuntime createStandaloneDirector(File installLocation, int forkedProcessTimeoutInSeconds)
            throws MojoExecutionException;

}
