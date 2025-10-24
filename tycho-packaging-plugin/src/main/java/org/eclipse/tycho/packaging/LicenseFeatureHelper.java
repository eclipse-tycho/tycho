/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.eclipse.tycho.model.Feature;

public interface LicenseFeatureHelper {

    File getLicenseFeature(Feature feature, MavenProject mavenProject);

    ArchivedFileSet getLicenseFeatureFileSet(File licenseFeature) throws IOException;
}
