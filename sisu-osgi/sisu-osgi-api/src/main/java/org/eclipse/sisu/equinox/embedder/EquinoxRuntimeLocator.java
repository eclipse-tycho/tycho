/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
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
package org.eclipse.sisu.equinox.embedder;

public interface EquinoxRuntimeLocator {

    default void locateRuntime(EquinoxRuntimeDescription description) throws Exception {
        locateRuntime(description, false);
    }

    public void locateRuntime(EquinoxRuntimeDescription description, boolean forked) throws Exception;

}
