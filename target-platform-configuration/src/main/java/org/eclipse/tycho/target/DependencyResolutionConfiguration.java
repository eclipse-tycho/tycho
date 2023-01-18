/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.target;

import java.util.List;
import java.util.Properties;

public class DependencyResolutionConfiguration {

	public class ExtraRequirementConfiguration {
		// Adapted from DefaultArtifactKey
		public String type;
		public String id;
		public String versionRange;
	}

	/**
	 * One of <code>ignore</code>, <code>optional</code>, <code>require</code>
	 */
	public String optionalDependencies;
    public List<ExtraRequirementConfiguration> extraRequirements;
    public Properties profileProperties;
}
