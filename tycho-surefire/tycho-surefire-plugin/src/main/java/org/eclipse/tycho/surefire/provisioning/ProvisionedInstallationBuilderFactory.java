/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.provisioning;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.maven.P2ApplicationLauncher;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

@Component(role = ProvisionedInstallationBuilderFactory.class)
public class ProvisionedInstallationBuilderFactory {

    @Requirement
    private BundleReader bundleReader;

    @Requirement(hint = TychoServiceFactory.HINT)
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
