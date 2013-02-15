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
package org.eclipse.tycho.p2.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class DependencyCollectorTest {

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Test
    public void missingDependencies() {
        DependencyCollector dc = new DependencyCollector(logVerifier.getLogger());

        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        String time = Long.toString(System.currentTimeMillis());
        iud.setId(time);
        iud.setVersion(Version.createOSGi(0, 0, 0, time));

        ArrayList<IRequirement> requirements = new ArrayList<IRequirement>();

        VersionRange range = new VersionRange("[1.2.3,1.2.4)");
        requirements.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "this.is.a.missing.iu",
                range, null, 1 /* min */, 1 /* max */, true /* greedy */));

        iud.setRequirements((IRequirement[]) requirements.toArray(new IRequirement[requirements.size()]));

        HashSet<IInstallableUnit> rootUIs = new HashSet<IInstallableUnit>();
        rootUIs.add(MetadataFactory.createInstallableUnit(iud));

        dc.setRootInstallableUnits(rootUIs);
        dc.setAdditionalRequirements(new ArrayList<IRequirement>());
        dc.setAvailableInstallableUnits(Collections.<IInstallableUnit> emptyList());

        try {
            dc.resolve(Collections.<String, String> emptyMap(), new NullProgressMonitor());
            Assert.fail();
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();

            Assert.assertTrue(cause instanceof ProvisionException);

            ProvisionException pe = (ProvisionException) cause;

            Assert.assertTrue(pe.getStatus().isMultiStatus());

            MultiStatus status = (MultiStatus) pe.getStatus();

            Assert.assertEquals(1, status.getChildren().length);

            Assert.assertTrue(e.toString().contains("this.is.a.missing.iu"));
        }
    }
}
