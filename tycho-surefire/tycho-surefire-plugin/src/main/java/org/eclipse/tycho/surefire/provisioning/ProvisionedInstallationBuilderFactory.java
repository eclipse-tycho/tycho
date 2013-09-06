/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.provisioning;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.launching.internal.P2ApplicationLauncher;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

@Component(role = ProvisionedInstallationBuilderFactory.class)
public class ProvisionedInstallationBuilderFactory {

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private EquinoxServiceFactory osgiServices;

    @Requirement
    private P2ApplicationLauncher launcher;

    @Requirement
    private Logger logger;

    public ProvisionedInstallationBuilder createInstallationBuilder() {
        return new ProvisionedInstallationBuilder(bundleReader, getDirectorRuntime(), launcher, logger);
    }

    private DirectorRuntime getDirectorRuntime() {
        return osgiServices.getService(DirectorRuntime.class);
    }

}
