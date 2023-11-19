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
package org.eclipse.tycho.eclipsetest;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This Mojo provides the <a href=
 * "https://wiki.eclipse.org/Platform-releng/Eclipse_Test_Framework">Eclipse
 * Test Framework</a> to maven and is a replacement for the <a href=
 * "https://wiki.eclipse.org/Platform-releng/Eclipse_Test_Framework#Headless_Testing_vs._UI_testing"><code>core-test</code></a>
 * ant target.
 */
@Mojo(name = "eclipse-core-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class EclipseCoreTestMojo extends AbstractEclipseTestMojo {

	@Override
	protected String getApplication() {
		return "org.eclipse.test.coretestapplication";
	}

}
