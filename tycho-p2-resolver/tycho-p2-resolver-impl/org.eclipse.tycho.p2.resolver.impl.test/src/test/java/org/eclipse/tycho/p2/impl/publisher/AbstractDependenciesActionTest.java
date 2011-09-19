/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import static org.junit.Assert.*;

import java.util.Set;

import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.p2.impl.publisher.AbstractDependenciesAction;
import org.junit.Before;
import org.junit.Test;

public class AbstractDependenciesActionTest {

    private AbstractDependenciesAction dependenciesAction;

    private static final class DependenciesAction extends AbstractDependenciesAction {
        @Override
        protected Version getVersion() {
            return null;
        }

        @Override
        protected Set<IRequirement> getRequiredCapabilities() {
            return null;
        }

        @Override
        protected String getId() {
            return null;
        }
    }

    @Before
    public void setup() {
        dependenciesAction = new DependenciesAction();
    }

    @Test
    public void testOsWsArchFilter() throws Exception {
        assertEquals("(& (osgi.os=test-os) (osgi.ws=test-ws) (osgi.arch=test-arch) )",
                dependenciesAction.getFilter("test-os", "test-ws", "test-arch"));
    }

    @Test
    public void testOsArchFilter() throws Exception {
        assertEquals("(& (osgi.os=test-os) (osgi.arch=test-arch) )",
                dependenciesAction.getFilter("test-os", null, "test-arch"));
    }

    @Test
    public void testOsFilter() throws Exception {
        assertEquals("(osgi.os=test-os)", dependenciesAction.getFilter("test-os", null, null));
    }
}
