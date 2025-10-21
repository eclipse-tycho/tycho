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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

@Named
@Singleton
public class DefaultProvisionedInstallationBuilderFactory implements ProvisionedInstallationBuilderFactory {

    @Inject
    private DirectorRuntime directorRuntime;

    @Inject
    private Logger logger;

    @Override
    public ProvisionedInstallationBuilder createInstallationBuilder() {
        return new ProvisionedInstallationBuilder(directorRuntime, logger);
    }

}
