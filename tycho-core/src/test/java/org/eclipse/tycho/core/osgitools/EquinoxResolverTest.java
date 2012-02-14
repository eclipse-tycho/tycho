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
package org.eclipse.tycho.core.osgitools;

import java.util.Properties;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultTargetPlatform;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.osgi.framework.BundleException;

public class EquinoxResolverTest extends AbstractTychoMojoTestCase {

    private EquinoxResolver subject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        subject = lookup(EquinoxResolver.class);
    }

    @Override
    protected void tearDown() throws Exception {
        subject = null;

        super.tearDown();
    }

    public void test_noSystemBundle() throws BundleException {
        Properties properties = subject.getPlatformProperties(new Properties(), null);
        State state = subject.newState(new DefaultTargetPlatform(), properties);

        BundleDescription[] bundles = state.getBundles("system.bundle");

        assertEquals(1, bundles.length);
    }
}
