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
package TYCHO0367localRepositoryCrosstalk.bundle02;

import junit.framework.TestCase;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class Eclipse35Test extends TestCase {
    public void test() throws Exception {
        Bundle equinox = getBundle("org.eclipse.osgi");

        assertEquals(3, equinox.getVersion().getMajor());
        assertEquals(17, equinox.getVersion().getMinor());
    }

    public Bundle getBundle(String id) {
        for (Bundle bundle : Activator.context.getBundles()) {
            if (id.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }

        throw new IllegalStateException();
    }
}
