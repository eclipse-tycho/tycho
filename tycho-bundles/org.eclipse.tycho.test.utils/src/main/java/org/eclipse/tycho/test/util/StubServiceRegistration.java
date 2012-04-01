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
    private T instance;

    private ServiceRegistration<T> serviceRegistration;

    /**
     * {@link Rule} that registers (and unregisters) the given instance as service of the given
     * type.
     * 
     * @param type
     *            The type of service to be registered.
     * @param instance
     *            The instance to be registered as service.
     */
    public StubServiceRegistration(Class<T> type, T instance) {
        this.type = type;
        this.instance = instance;
    }

    /**
     * {@link Rule} that can be used to register a service of the given type.
     * 
     * @param type
     *            The type of service to be registered.
     * @see #registerService(T)
     */
    public StubServiceRegistration(Class<T> type) {
        this.type = type;
        this.instance = null;
    }

    @Override
    protected void before() throws Throwable {
        if (instance != null) {
            internalRegisterService();
        }
    }

    public void registerService(T instance) {
        this.instance = instance;
        internalRegisterService();
    }

    private void internalRegisterService() {
        if (serviceRegistration != null) {
            throw new IllegalStateException("This instance can only register one service");
        }
        serviceRegistration = Activator.getContext().registerService(type, instance, null);
    }

    @Override
    protected void after() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        } else {
            String classNameWithGenericParameter = this.getClass().getSimpleName() + "<" + type.getSimpleName() + ">";
            System.out
                    .println("Warning: "
                            + classNameWithGenericParameter
                            + " instance did not register a service; provide an implementation in the constructor or via setService");
        }
    }
}
