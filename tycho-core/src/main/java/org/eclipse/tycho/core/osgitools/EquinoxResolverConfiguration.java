/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.util.concurrent.TimeUnit;

final class EquinoxResolverConfiguration {
    //The following properties are not supported for general usage and intended to experimental testing when developing Tycho, so use with care

    public EquinoxResolverConfiguration() {
        keepUses = Boolean.getBoolean("tycho.equinox.resolver.uses");
        batchSize = System.getProperty("tycho.equinox.resolver.batch.size", keepUses ? null : "1");
    }

    public EquinoxResolverConfiguration(EquinoxResolverConfiguration source, boolean forceKeepUses) {
        keepUses = forceKeepUses;
        batchSize = keepUses ? null : source.batchSize;
    }

    /**
     * If set to true this keep the 'uses' constraints of a package, this will make it more hard for
     * the resolver or even let him fail to compute a solution if different package providers are
     * present
     */
    final boolean keepUses;

    /**
     * Keep the default batch size, but allow to override this if necessary, if 'uses' constrains
     * are kept, do not restrict the batch size as this potentially fails the resolve
     */
    final String batchSize;

    /**
     * Set the batch timeout to an acceptable timeout before fallback to resolve one bundle at a
     * time, but allow to override this if necessary
     */
    final static String BATCH_TIMEOUT = System.getProperty("tycho.equinox.resolver.batch.timeout",
            String.valueOf(TimeUnit.SECONDS.toMillis(30)));

    /**
     * Allow to adjust the default thread count used in resolver operations
     */
    final static int THREAD_COUNT = Integer.getInteger("tycho.equinox.resolver.executor.threads", 1);
}
