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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.util.Collections;
import java.util.List;

import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.ClasspathEntry.AccessRule;

public class BundleClassPath {

    private final List<ClasspathEntry> classpath;
    private final List<AccessRule> strictBootClasspathAccessRules;
    private final List<AccessRule> bootClasspathExtraAccessRules;

    BundleClassPath(List<ClasspathEntry> classpath, List<AccessRule> strictBootClasspathAccessRules,
            List<AccessRule> bootClasspathExtraAccessRules) {
        this.classpath = Collections.unmodifiableList(classpath);
        this.strictBootClasspathAccessRules = Collections.unmodifiableList(strictBootClasspathAccessRules);
        this.bootClasspathExtraAccessRules = Collections.unmodifiableList(bootClasspathExtraAccessRules);
    }

    public List<ClasspathEntry> getClasspathEntries() {
        return classpath;
    }

    public List<AccessRule> getStrictBootClasspathAccessRules() {
        return strictBootClasspathAccessRules;
    }

    public List<AccessRule> getExtraBootClasspathAccessRules() {
        return bootClasspathExtraAccessRules;
    }

}
