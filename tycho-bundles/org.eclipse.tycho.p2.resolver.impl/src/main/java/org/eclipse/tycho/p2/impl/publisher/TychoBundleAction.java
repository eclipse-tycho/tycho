/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;

@SuppressWarnings("restriction")
public class TychoBundleAction extends BundlesAction {

    public TychoBundleAction(File location) {
        super(new File[] { location });
    }

    public TychoBundleAction(BundleDescription bundleDescription) {
        super(new BundleDescription[] { bundleDescription });
    }

    @Override
    protected BundleDescription[] getBundleDescriptions(File[] bundleLocations, IProgressMonitor monitor) {
        /*
         * For reasons that I don't quite understand, p2 publisher BundlesAction generates two IUs
         * for org.eclipse.update.configurator bundle, the extra IU matching
         * org.eclipse.equinox.simpleconfigurator bundle. The extra IU results in wrong target
         * platform resolution for projects that depend on org.eclipse.equinox.simpleconfigurator
         * bundle or packages provided by it.
         * 
         * The solution is to suppress special handling of org.eclipse.update.configurator bundle
         * when generating p2 metadata of reactor projects and from what I can tell, this is
         * consistent with PDE behaviour (see
         * org.eclipse.pde.internal.build.publisher.GatherBundleAction ).
         */

        BundleDescription[] result = new BundleDescription[bundleLocations.length];
        for (int i = 0; i < bundleLocations.length; i++) {
            if (monitor.isCanceled())
                throw new OperationCanceledException();
            result[i] = createBundleDescription(bundleLocations[i]);
        }
        return result;
    }

}
