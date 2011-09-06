/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
