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
package org.eclipse.tycho.p2.impl.publisher;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.p2.updatesite.SiteModel;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;

@SuppressWarnings("restriction")
public class SiteDependenciesAction extends AbstractSiteDependenciesAction {
    private final File location;

    private UpdateSite updateSite;

    public SiteDependenciesAction(File location, String id, String version) {
        super(id, version);
        this.location = location;
    }

    @Override
    SiteModel getSiteModel() {
        return updateSite.getSite();
    }

    @Override
    public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
        try {
            Transport transport = null; // don't need transport to read local site.xml
            updateSite = UpdateSite.load(location.toURI(), transport, monitor);
        } catch (ProvisionException e) {
            return new Status(IStatus.ERROR, Activator.ID, "Error generating site xml action.", e);
        }

        return super.perform(publisherInfo, results, monitor);
    }

}
