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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

/**
 * Collects constants used in this plugin
 *
 */
public interface P2Plugin {

	String GROUP_ID = "org.eclipse.tycho";
	String ARTIFACT_ID = "p2-maven-plugin";
	String KEY = GROUP_ID + ":" + ARTIFACT_ID;
	String BUNDLE_ID = GROUP_ID + "." + ARTIFACT_ID.replace('-', '.');
}
