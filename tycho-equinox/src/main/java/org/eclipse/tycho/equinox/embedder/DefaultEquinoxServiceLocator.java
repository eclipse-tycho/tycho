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
package org.eclipse.tycho.equinox.embedder;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.equinox.EquinoxServiceFactory;

@Component(role = EquinoxServiceFactory.class)
public class DefaultEquinoxServiceLocator implements EquinoxServiceFactory {
    @Requirement
    private EquinoxEmbedder equinox;

    public <T> T getService(Class<T> clazz) {
        return equinox.getService(clazz);
    }

    public <T> T getService(Class<T> clazz, String filter) {
        return equinox.getService(clazz, filter);
    }
}
