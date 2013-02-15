/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.resolver;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.matchers.JUnitMatchers.containsString;

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
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.test.util.InstallableUnitUtil;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("restriction")
public class ProjectorResolutionStrategyTest {

    private static final class CollectionQueryable implements IQueryable<IInstallableUnit> {

        private final Collection<IInstallableUnit> ius;

        private CollectionQueryable(Collection<IInstallableUnit> ius) {
            this.ius = ius;
        }

        public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
            return query.perform(ius.iterator());
        }
    }

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    private ProjectorResolutionStrategy strategy;

    @Before
    public void setup() {
        strategy = new ProjectorResolutionStrategy(logVerifier.getLogger());
        strategy.setRootInstallableUnits(Collections.<IInstallableUnit> emptyList());
    }

    @Test
    public void testFixSwtWithNLSFragmentPresent() throws Exception {
        final List<IInstallableUnit> selectedIUs = createSwtHostBundleIUList();
        IInstallableUnit swtImplFragment = createSwtFragment("linux", "gtk", "x86_64", null);
        IInstallableUnit swtNLSFragment = createSwtFragment("linux", "gtk", "x86_64", "de");
        final List<IInstallableUnit> availableIUs = new ArrayList<IInstallableUnit>();
        availableIUs.addAll(selectedIUs);
        availableIUs.add(swtNLSFragment);
        availableIUs.add(swtImplFragment);
        strategy.fixSWT(new CollectionQueryable(availableIUs), selectedIUs,
                createSelectionContext("linux", "gtk", "x86_64"), new NullProgressMonitor());
        assertThat(selectedIUs.size(), is(2));
        assertThat(selectedIUs, hasItem(swtImplFragment));
    }

    @Test
    public void testFixSwtNoSwtDependency() throws Exception {
        final List<IInstallableUnit> selectedIUs = new ArrayList<IInstallableUnit>();
        IInstallableUnit swtImplFragment = createSwtFragment("linux", "gtk", "x86_64", null);
        final List<IInstallableUnit> availableIUs = new ArrayList<IInstallableUnit>();
        availableIUs.add(swtImplFragment);
        strategy.fixSWT(new CollectionQueryable(availableIUs), selectedIUs,
                createSelectionContext("linux", "gtk", "x86_64"), new NullProgressMonitor());
        assertThat(selectedIUs.size(), is(0));
    }

    @Test
    public void testFixSwtNoImplFound() throws Exception {
        final List<IInstallableUnit> selectedIUs = createSwtHostBundleIUList();
        // fragment does not match selection context
        IInstallableUnit swtImplFragmentWindows = createSwtFragment("win32", "win32", "x86_64", null);
        final List<IInstallableUnit> availableIUs = new ArrayList<IInstallableUnit>();
        availableIUs.addAll(selectedIUs);
        availableIUs.add(swtImplFragmentWindows);
        try {
            strategy.fixSWT(new CollectionQueryable(availableIUs), selectedIUs,
                    createSelectionContext("linux", "gtk", "x86_64"), new NullProgressMonitor());
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
        final List<IInstallableUnit> selectedIUs = createSwtHostBundleIUList();
        invokefixSwtWithLinuxFragmentPresent(rootIU, selectedIUs);
        assertThat(selectedIUs.size(), is(1));
        assertThat(selectedIUs, hasItem(InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0")));
    }

    @Test
    public void testFixSwtSwtFragmentInRootIUs() throws Exception {
        IInstallableUnit rootIU = createSwtFragment("linux", "gtk", "x86_64", null);
        final List<IInstallableUnit> selectedIUs = createSwtHostBundleIUList();
        invokefixSwtWithLinuxFragmentPresent(rootIU, selectedIUs);
        assertThat(selectedIUs.size(), is(1));
        assertThat(selectedIUs, hasItem(InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0")));
    }

    private void invokefixSwtWithLinuxFragmentPresent(IInstallableUnit rootIU, final List<IInstallableUnit> selectedIUs) {
        strategy.setRootInstallableUnits(Collections.singleton(rootIU));
        final List<IInstallableUnit> availableIUs = new ArrayList<IInstallableUnit>();
        IInstallableUnit swtImplFragment = createSwtFragment("linux", "gtk", "x86_64", null);
        availableIUs.addAll(selectedIUs);
        availableIUs.add(swtImplFragment);
        strategy.fixSWT(new CollectionQueryable(availableIUs), selectedIUs,
                createSelectionContext("linux", "gtk", "x86_64"), new NullProgressMonitor());
    }

    private List<IInstallableUnit> createSwtHostBundleIUList() {
        final List<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();
        IInstallableUnit swtHost = InstallableUnitUtil.createIU("org.eclipse.swt", "1.0.0");
        ius.add(swtHost);
        return ius;
    }

    private IInstallableUnit createSwtFragment(String os, String ws, String arch, String nls) {
        Map<String, String> capsMap = new HashMap<String, String>();
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
        IInstallableUnit linuxSwtFragment = InstallableUnitUtil.createIUWithCapabilitiesAndFilter("org.eclipse.swt."
                + iuSuffix, "1.0.0", createCapabilities(capsMap), "(&(osgi.arch=" + arch + ")(osgi.os=" + os
                + ")(osgi.ws=" + ws + "))");
        return linuxSwtFragment;
    }

    private static HashMap<String, String> createSelectionContext(String os, String ws, String arch) {
        HashMap<String, String> selectionContext = new HashMap<String, String>();
        selectionContext.put("osgi.os", os);
        selectionContext.put("osgi.ws", ws);
        selectionContext.put("osgi.arch", arch);
        return selectionContext;
    }

    private static Collection<IProvidedCapability> createCapabilities(Map<String, String> namespace2NameMap) {
        List<IProvidedCapability> result = new ArrayList<IProvidedCapability>();
        for (Map.Entry<String, String> entry : namespace2NameMap.entrySet()) {
            result.add(new ProvidedCapability(entry.getKey(), entry.getValue(), Version.create("1.0.0")));
        }
        return result;
    }
}
