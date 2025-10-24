/*******************************************************************************
 * Copyright (c) 2015 Tasktop Technologies and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.tycho.ArtifactDescriptor;

/**
 * A helper for discovering common timestamps in strings
 */
public interface TimestampFinder {

	Date findByDescriptor(ArtifactDescriptor artifact, SimpleDateFormat format);

	Date findInString(String string);
}
