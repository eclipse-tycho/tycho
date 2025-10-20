/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This mojo is only there to provide configuration parameters and should not be executed directly.
 */
@Mojo(name = "configure-document-bundle-plugin", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class ConfigureMojo extends AbstractMojo {

	/**
	 * name of the parameter to inject javadoc source dependencies
	 */
	public static final String PARAM_INJECT_JAVADOC_DEPENDENCIES = "injectJavadocDependencies";
	@Parameter(name = PARAM_INJECT_JAVADOC_DEPENDENCIES)
	private boolean dummyBoolean;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		throw new MojoFailureException("This mojo is not intended to be ever called");
	}

}
