/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.test.util.MavenServiceStubbingTestBase;
import org.junit.Test;
import org.osgi.util.tracker.ServiceTracker;

public class PublisherServiceFactoryTest extends MavenServiceStubbingTestBase {

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

}
