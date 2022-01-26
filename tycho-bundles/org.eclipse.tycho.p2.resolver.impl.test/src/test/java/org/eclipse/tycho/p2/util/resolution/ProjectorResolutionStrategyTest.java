/*******************************************************************************
 * Copyright (c) 2013, 2014 SAP SE and others.
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

package org.eclipse.tycho.p2.util.resolution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils;
import org.eclipse.tycho.p2.testutil.InstallableUnitUtil;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;

@SuppressWarnings("restriction")
public class ProjectorResolutionStrategyTest {

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();
    private IProgressMonitor monitor = new NullProgressMonitor();

    private ProjectorResolutionStrategy strategy;
    private ResolutionDataImpl data = new ResolutionDataImpl(ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HINTS);

    private List<IInstallableUnit> selectedIUs;

    @Before
    public void setup() {
        strategy = new ProjectorResolutionStrategy(logVerifier.getLogger());
        strategy.setData(data);
        data.setRootIUs(Collections.<IInstallableUnit> emptyList());

        selectedIUs = new ArrayList<>();
    }

    private IInstallableUnit createSwtFragment(String os, String ws, String arch, String nls) {
        Map<String, String> capsMap = new HashMap<>();
        capsMap.put("osgi.fragment", "org.eclipse.swt");
        if (nls == null) {
            // NLS fragments do not provide packages
            capsMap.put("java.package", "foo");
        }
        String iuSuffix;
        iuSuffix = os + "." + ws + "." + arch;
        if (nls != null) {
            iuSuffix += "." + nls;
        }
        IInstallableUnit linuxSwtFragment = InstallableUnitUtil.createIUWithCapabilitiesAndFilter(
                "org.eclipse.swt." + iuSuffix, "1.0.0", createCapabilities(capsMap),
                "(&(osgi.arch=" + arch + ")(osgi.os=" + os + ")(osgi.ws=" + ws + "))");
        return linuxSwtFragment;
    }

    private static HashMap<String, String> createSelectionContext(String os, String ws, String arch) {
        HashMap<String, String> selectionContext = new HashMap<>();
        selectionContext.put("osgi.os", os);
        selectionContext.put("osgi.ws", ws);
        selectionContext.put("osgi.arch", arch);
        return selectionContext;
    }

    private static Collection<IProvidedCapability> createCapabilities(Map<String, String> namespace2NameMap) {
        List<IProvidedCapability> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : namespace2NameMap.entrySet()) {
            result.add(new ProvidedCapability(entry.getKey(), entry.getValue(), Version.create("1.0.0")));
        }
        return result;
    }
}
