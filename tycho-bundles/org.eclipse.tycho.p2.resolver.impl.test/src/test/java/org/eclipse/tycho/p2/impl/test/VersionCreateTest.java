/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.impl.test;

import org.eclipse.tycho.p2.impl.publisher.SiteDependenciesAction;
import org.junit.Test;

public class VersionCreateTest {
    @Test
    public void test() throws Exception {
        SiteDependenciesAction.createSiteVersion("0.10.0"); // maven RELEASE version
        SiteDependenciesAction.createSiteVersion("0.10.0-SNAPSHOT"); // maven SNAPSHOT version
        SiteDependenciesAction.createSiteVersion("0.10.0.20100205-2200"); // maven RELEASE version
        SiteDependenciesAction.createSiteVersion("0.10.0.qualifier"); // maven RELEASE version
    }
}
