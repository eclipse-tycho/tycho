/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.impl.publisher;

import java.io.File;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.publisher.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.PublisherServiceFactory;

public class PublisherServiceFactoryImpl implements PublisherServiceFactory {

    public PublisherService createPublisher(File targetRepository, RepositoryReferences contextRepos,
            BuildContext context) throws FacadeException {
        /*
         * Create an own instance of the provisioning agent to prevent cross talk with other things
         * that happen in the Tycho OSGi runtime. We can assume to own the directory into which the
         * agent writes its cache.
         */
        IProvisioningAgent agent = Activator.createProvisioningAgent(context.getTargetDirectory());

        return new PublisherServiceImpl(context, new PublisherInfoTemplate(targetRepository, contextRepos, context,
                agent));
    }
}
