/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.ds;

import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.DeclarativeServicesConfiguration;

public interface DeclarativeServiceConfigurationReader {

    String DEFAULT_ENABLED = "false";
    String DEFAULT_ADD_TO_CLASSPATH = "true";
    String DEFAULT_DS_VERSION = "1.4";
    String DEFAULT_PATH = "OSGI-INF";

    DeclarativeServicesConfiguration getConfiguration(ReactorProject reactorProject) throws IOException;

    DeclarativeServicesConfiguration getConfiguration(MavenProject mavenProject) throws IOException;

}
