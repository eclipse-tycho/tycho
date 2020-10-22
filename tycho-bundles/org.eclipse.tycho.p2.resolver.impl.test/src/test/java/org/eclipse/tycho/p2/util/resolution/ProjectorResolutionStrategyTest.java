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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

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
import org.junit.Test;

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

    @Test
    public void testFixSwt() throws Exception {
        selectedIUs.add(InstallableUnitUtil.createIU("org.eclipse.swt", "3.103.1.v20140903-1938")); // a Luna version
        IInstallableUnit swtImplFragment = createSwtFragment("linux", "gtk", "x86_64", null);
        final List<IInstallableUnit> availableIUs = new ArrayList<>();
        availableIUs.addAll(selectedIUs);
        availableIUs.add(swtImplFragment);
        strategy.fixSWT(availableIUs, selectedIUs, createSelectionContext("linux", "gtk", "x86_64"), monitor);
        assertThat(selectedIUs.size(), is(2));
        assertThat(selectedIUs, hasItem(swtImplFragment));
    }

    @Test
    public void testFixSwtDisabledForNonBrokenSWTVersion() throws Exception {
        selectedIUs.add(InstallableUnitUtil.createIU("org.eclipse.swt", "3.104.0.v20141125-0639")); // SWT bug 361901 is fixed since Mars
        IInstallableUnit swtImplFragment = createSwtFragment("linux", "gtk", "x86_64", null);
        final List<IInstallableUnit> availableIUs = new ArrayList<>();
        availableIUs.addAll(selectedIUs);
        availableIUs.add(swtImplFragment);
        // this is a synthetic setup to test that fixSWT doesn't do anything -> it doesn't need to do anything because the selectedIUs would already contain the right fragment
        strategy.fixSWT(availableIUs, selectedIUs, createSelectionContext("linux", "gtk", "x86_64"), monitor);
        assertThat(selectedIUs.size(), is(1));
        assertThat(selectedIUs, not(hasItem(swtImplFragment)));
    }

    @Test
    public void testFixSwtWithNLSFragmentPresent() throws Exception {
        selectedIUs.add(InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0"));
        IInstallableUnit swtImplFragment = createSwtFragment("linux", "gtk", "x86_64", null);
        IInstallableUnit swtNLSFragment = createSwtFragment("linux", "gtk", "x86_64", "de");
        final List<IInstallableUnit> availableIUs = new ArrayList<>();
        availableIUs.addAll(selectedIUs);
        availableIUs.add(swtNLSFragment);
        availableIUs.add(swtImplFragment);
        strategy.fixSWT(availableIUs, selectedIUs, createSelectionContext("linux", "gtk", "x86_64"), monitor);
        assertThat(selectedIUs.size(), is(2));
        assertThat(selectedIUs, hasItem(swtImplFragment));
    }

    @Test
    public void testFixSwtNoSwtDependency() throws Exception {
        IInstallableUnit swtImplFragment = createSwtFragment("linux", "gtk", "x86_64", null);
        final List<IInstallableUnit> availableIUs = new ArrayList<>();
        availableIUs.add(swtImplFragment);
        strategy.fixSWT(availableIUs, selectedIUs, createSelectionContext("linux", "gtk", "x86_64"), monitor);
        assertThat(selectedIUs.size(), is(0));
    }

    @Test
    public void testFixSwtNoImplFound() throws Exception {
        selectedIUs.add(InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0"));
        // fragment does not match selection context
        IInstallableUnit swtImplFragmentWindows = createSwtFragment("win32", "win32", "x86_64", null);
        final List<IInstallableUnit> availableIUs = new ArrayList<>();
        availableIUs.addAll(selectedIUs);
        availableIUs.add(swtImplFragmentWindows);
        try {
            strategy.fixSWT(availableIUs, selectedIUs, createSelectionContext("linux", "gtk", "x86_64"), monitor);
            fail();
        } catch (RuntimeException e) {
            // expected
            assertThat(e.getMessage(),
                    containsString("Could not determine SWT implementation fragment bundle for environment"));
        }
    }

    @Test
    public void testFixSwtSwtInRootIUs() throws Exception {
        IInstallableUnit rootIU = InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0");
        selectedIUs.add(InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0"));
        invokefixSwtWithLinuxFragmentPresent(rootIU, selectedIUs);
        assertThat(selectedIUs.size(), is(1));
        assertThat(selectedIUs, hasItem(InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0")));
    }

    @Test
    public void testFixSwtSwtFragmentInRootIUs() throws Exception {
        IInstallableUnit rootIU = createSwtFragment("linux", "gtk", "x86_64", null);
        selectedIUs.add(InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0"));
        invokefixSwtWithLinuxFragmentPresent(rootIU, selectedIUs);
        assertThat(selectedIUs.size(), is(1));
        assertThat(selectedIUs, hasItem(InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0")));
    }

    private void invokefixSwtWithLinuxFragmentPresent(IInstallableUnit rootIU,
            final List<IInstallableUnit> selectedIUs) {
        data.setRootIUs(Collections.singleton(rootIU));
        final List<IInstallableUnit> availableIUs = new ArrayList<>();
        IInstallableUnit swtImplFragment = createSwtFragment("linux", "gtk", "x86_64", null);
        availableIUs.addAll(selectedIUs);
        availableIUs.add(swtImplFragment);
        strategy.fixSWT(availableIUs, selectedIUs, createSelectionContext("linux", "gtk", "x86_64"), monitor);
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
