/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.Collection;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.QueryUtil;

@SuppressWarnings("restriction")
class JREInstallableUnits {

    private final String executionEnvironment;

    public JREInstallableUnits(String executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }

    /**
     * p2 repositories are polluted with useless a.jre/config.a.jre IUs. These IUs do not represent
     * current/desired JRE and can expose resolver to packages that are not actually available.
     */
    public boolean isJREUI(IInstallableUnit iu) {
        // See JREAction
        return iu.getId().startsWith("a.jre") || iu.getId().startsWith("config.a.jre");
    }

    /**
     * Return IUs that represent packages provided by target JRE
     * 
     * @param executionEnvironment
     */
    public Collection<IInstallableUnit> getJREIUs() {
        PublisherResult results = new PublisherResult();
        new JREAction(executionEnvironment).perform(new PublisherInfo(), results, new NullProgressMonitor());
        return results.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toUnmodifiableSet();
    }
}
