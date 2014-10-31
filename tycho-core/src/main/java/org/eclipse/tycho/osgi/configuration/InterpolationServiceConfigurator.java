/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.shared.InterpolationService;

@Component(role = EquinoxLifecycleListener.class, hint = "InterpolationServiceConfigurator")
public class InterpolationServiceConfigurator extends EquinoxLifecycleListener {

    @Requirement
    private InterpolationService interpolationService;

    /**
     * Registers the {@link InterpolationService} plexus component as an OSGi service so it can be
     * used from OSGi too.
     */
    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(InterpolationService.class, interpolationService);
    }

}
