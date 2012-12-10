/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.eclipse.tycho.test.util.MemoryLog;
import org.eclipse.tycho.test.util.StubServiceRegistration;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.util.tracker.ServiceTracker;

public class PublisherServiceFactoryTest {

    // in the productive code, these services are provided from outside the OSGi runtime
    @Rule
    public StubServiceRegistration<MavenContext> mavenContextRegistration = new StubServiceRegistration<MavenContext>(
            MavenContext.class, createMavenContext());
    @Rule
    public StubServiceRegistration<BuildPropertiesParser> buildPropertiesParserRegistration = new StubServiceRegistration<BuildPropertiesParser>(
            BuildPropertiesParser.class, new BuildPropertiesParserForTesting());

    @Test
    public void testThatRequiredServicesAreAvailable() throws Exception {
        ServiceTracker<PublisherServiceFactory, PublisherServiceFactory> tracker = new ServiceTracker<PublisherServiceFactory, PublisherServiceFactory>(
                Activator.getContext(), PublisherServiceFactory.class, null);
        tracker.open();
        try {
            PublisherServiceFactory publisherServiceFactory = tracker.waitForService(2000);
            // factory service is only available if all required services are available
            assertThat(publisherServiceFactory, is(notNullValue()));
        } finally {
            tracker.close();
        }
    }

    private static MavenContext createMavenContext() {
        MavenContext mavenContext = new MavenContextImpl(null, false, new MemoryLog(), null);
        return mavenContext;
    }

}
