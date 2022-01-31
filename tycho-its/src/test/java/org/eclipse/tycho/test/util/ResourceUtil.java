/*******************************************************************************
 * Copyright (c) 2011, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.net.URI;

/**
 * Helper for accessing test resources.
 */
public class ResourceUtil {

	public enum P2Repositories {
		ECLIPSE_342("e342"), ECLIPSE_352("e352"), ECLIPSE_OXYGEN("https:////download.eclipse.org/releases/oxygen/"),
		ECLIPSE_LATEST("https:////download.eclipse.org/releases/2021-12/");

		private final String path;

		P2Repositories(String path) {
			this.path = path;
		}

		public URI getResolvedLocation() throws IllegalStateException {
			if (path.startsWith("https:") || path.startsWith("http:")) {
				return URI.create(path);
			}
			return resolveTestResource("repositories/" + path).toURI();
		}

		@Override
		public String toString() {
			return getResolvedLocation().toString();
		}
	}

	public static File resolveTestResource(String pathRelativeToProjectRoot) throws IllegalStateException {
		File resolvedFile = new File(pathRelativeToProjectRoot).getAbsoluteFile();

		if (!resolvedFile.canRead()) {
			throw new IllegalStateException(
					"Test resource \"" + pathRelativeToProjectRoot + "\" is not available; " + workingDirMessage());
		}
		return resolvedFile;
	}

	private static String workingDirMessage() {
		return "(working directory is \"" + new File(".").getAbsolutePath() + "\")";
	}
}
