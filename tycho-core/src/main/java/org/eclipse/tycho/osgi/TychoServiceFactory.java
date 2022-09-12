/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;

/**
 * A special hinted component that could be used by Tycho Plugins to get a service factory suitable
 * for acquiring Tycho services.
 */
@Component(role = EquinoxServiceFactory.class, hint = TychoServiceFactory.HINT)
public class TychoServiceFactory implements EquinoxServiceFactory {

    public static final String HINT = "tycho-core";
    @Requirement(hint = "connect")
    EquinoxServiceFactory delegate;

    @Override
    public <T> T getService(Class<T> clazz) {
        return delegate.getService(clazz);
    }

    @Override
    public <T> T getService(Class<T> clazz, String filter) {
        return delegate.getService(clazz, filter);
    }

}
