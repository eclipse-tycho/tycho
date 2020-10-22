/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.artifactcomparator;


/**
 * Represents both simple and compound artifact delta.
 */
public interface ArtifactDelta {

    /**
     * @return description of the delta, never null.
     */
    public String getMessage();

    /**
     * @return detailed description of the delta, never null.
     */
    public String getDetailedMessage();

}
