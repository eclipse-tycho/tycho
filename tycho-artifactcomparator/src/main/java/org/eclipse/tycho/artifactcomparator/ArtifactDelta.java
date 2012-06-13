/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
