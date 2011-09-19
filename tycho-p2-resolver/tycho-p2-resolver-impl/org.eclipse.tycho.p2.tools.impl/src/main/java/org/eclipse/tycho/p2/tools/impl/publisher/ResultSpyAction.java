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

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.Publisher;

/**
 * This publisher action does nothing but storing the root IUs of the IPublisherResult instance used
 * by the publisher. This is a workaround for missing getters in {@link Publisher}.
 */
// TODO Provide patch to Eclipse to get rid of this workaround
@SuppressWarnings("restriction")
public class ResultSpyAction implements IPublisherAction {
    private Collection<IInstallableUnit> rootIUs = null;

    private Collection<IInstallableUnit> allIUs = null;

    public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
        if (wasPerformed()) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " cannot be performed more than once");
        }
        rootIUs = results.getIUs(null, IPublisherResult.ROOT);
        allIUs = results.getIUs(null, null);
        return Status.OK_STATUS;
    }

    /**
     * Returns the root IUs in the publisher result at the time when this action was invoked by the
     * {@link Publisher}.
     * 
     * @throws IllegalStateException
     *             if the action has not been performed.
     */
    public Collection<IInstallableUnit> getRootIUs() throws IllegalStateException {
        checkPerformed();
        return rootIUs;
    }

    /**
     * Returns all IUs in the publisher result at the time when this action was invoked by the
     * {@link Publisher}.
     * 
     * @throws IllegalStateException
     *             if the action has not been performed.
     */
    public Collection<IInstallableUnit> getAllIUs() throws IllegalStateException {
        checkPerformed();
        return allIUs;
    }

    private boolean wasPerformed() {
        return rootIUs != null;
    }

    private void checkPerformed() throws IllegalStateException {
        if (!wasPerformed()) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " has not been performed");
        }
    }
}
