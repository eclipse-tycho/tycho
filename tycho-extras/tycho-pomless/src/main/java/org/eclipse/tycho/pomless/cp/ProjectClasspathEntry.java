/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless.cp;

import java.util.Map;

public interface ProjectClasspathEntry {

    Map<String, String> getAttributes();

    /**
     * @return <code>true</code> if this entry is marked a being a test entry
     */
    default boolean isTest() {
        return Boolean.parseBoolean(getAttributes().get("test"));
    }
}
