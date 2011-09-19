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
package org.eclipse.tycho.p2.tools.impl.mirroring;

import org.eclipse.equinox.p2.core.IProvisioningAgent;

/**
 * {@link org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication} that uses a custom
 * {@link IProvisioningAgent}.
 */
@SuppressWarnings("restriction")
public class MirrorApplication extends org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication {
    public MirrorApplication(IProvisioningAgent agent) {
        super();
        this.agent = agent;
        this.removeAddedRepositories = false;
    }
}
