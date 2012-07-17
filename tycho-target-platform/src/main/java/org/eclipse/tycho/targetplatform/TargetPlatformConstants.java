/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat). - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.targetplatform;

import org.apache.maven.project.MavenProjectHelper;

/**
 * @author Mickael Istria (Red Hat Inc.)
 */
public interface TargetPlatformConstants {

	public static final String FILE_EXTENSION = ".target";
	public static final String TARGET_PLATFORM_PACKAGING = "eclipse-target-platform";
	/**
	 * To be used by {@link MavenProjectHelper#attachArtifact(org.apache.maven.project.MavenProject, String, String, java.io.File)}
	 */
	public static final String ATTACHED_TARGET_ARTIFACT_TYPE = "target";
	
}
