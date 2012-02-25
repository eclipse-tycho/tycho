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
package org.eclipse.tycho.test.util;

import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.osgi.framework.ServiceRegistration;

/**
 * Helper class to be used with JUnit's {@link Rule} annotation that allows to register an OSGi
 * service for the duration of the test.
 */
public class StubServiceRegistration<T> extends ExternalResource {

    private final Class<T> type;
    private final T instance;

    private ServiceRegistration<T> serviceRegistration;

    public StubServiceRegistration(Class<T> type, T instance) {
        this.type = type;
        this.instance = instance;
    }

    @Override
    protected void before() throws Throwable {
        serviceRegistration = Activator.getContext().registerService(type, instance, null);
    }

    @Override
    protected void after() {
        serviceRegistration.unregister();
    }
}
