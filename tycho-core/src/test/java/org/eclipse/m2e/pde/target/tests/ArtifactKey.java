/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Hannes Wellmann - Convert to record
 *******************************************************************************/

package org.eclipse.m2e.pde.target.tests;

import java.io.Serializable;

import org.eclipse.osgi.util.NLS;

public record ArtifactKey(String groupId, String artifactId, String version, String classifier)
    implements Serializable {
	public static ArtifactKey fromPortableString(String str) {
		int p = 0;
		int c = nextColonIndex(str, p);
		String groupId = substring(str, p, c);

		p = c + 1;
		c = nextColonIndex(str, p);
		String artifactId = substring(str, p, c);

		p = c + 1;
		c = nextColonIndex(str, p);
		String version = substring(str, p, c);

		p = c + 1;
		c = nextColonIndex(str, p);
		String classifier = substring(str, p, c);

		return new ArtifactKey(groupId, artifactId, version, classifier);
	}

	private static String substring(String str, int start, int end) {
		String substring = str.substring(start, end);
		return "".equals(substring) ? null : substring; //$NON-NLS-1$
	}

	private static int nextColonIndex(String str, int pos) {
		int idx = str.indexOf(':', pos);
		if (idx < 0) {
			throw new IllegalArgumentException(NLS.bind("Invalid portable string: {0}", str));
		}
		return idx;
	}
}
