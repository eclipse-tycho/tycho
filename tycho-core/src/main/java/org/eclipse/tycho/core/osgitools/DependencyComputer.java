/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    Sonatype Inc. - adopted to work outside of eclipse workspace
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.osgi.internal.resolver.ExportPackageDescriptionImpl;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.osgi.framework.Constants;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper class that computes compile dependencies of a bundle project.
 * 
 * Code below is copy&paste of org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer
 * adopted to work outside of Eclipse runtime.
 * 
 * Note that some functionality, namely SecondaryDependencies, ExtraClasspathEntries and
 * isPatchFragment, has been removed due to time constraints.
 */
@Component(role = DependencyComputer.class)
public class DependencyComputer {

    @Requirement
    private BundleReader manifestReader;

    public static class DependencyEntry {
        public final BundleDescription desc;
        public final Collection<AccessRule> rules;

        public DependencyEntry(BundleDescription desc, Collection<AccessRule> rules) {
            this.desc = desc;
            this.rules = rules;
        }

        @Override
        public int hashCode() {
            return Objects.hash(desc, rules);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DependencyEntry other = (DependencyEntry) obj;
            return Objects.equals(this.desc, other.desc) && Objects.equals(this.rules, other.rules);
        }

    }

    public List<DependencyEntry> computeDependencies(StateHelper helper, BundleDescription desc) {
        ArrayList<DependencyEntry> entries = new ArrayList<>();

        if (desc == null)
            return entries;

        Multimap<BundleDescription, AccessRule> map = retrieveVisiblePackagesFromState(helper, desc);

        HashSet<BundleDescription> added = new HashSet<>();

        // to avoid cycles, e.g. when a bundle imports a package it exports
        added.add(desc);

        HostSpecification host = desc.getHost();
        if (host != null) {
            addHostPlugin(host, added, map, entries);
        }

        // add dependencies
        BundleSpecification[] required = desc.getRequiredBundles();
        for (BundleSpecification required1 : required) {
            addDependency((BundleDescription) required1.getSupplier(), added, map, entries);
        }

        // add Import-Package
        // sort by symbolicName_version to get a consistent order
        Map<String, BundleDescription> sortedMap = new TreeMap<>();
        for (BundleDescription bundle : map.keySet()) {
            sortedMap.put(bundle.toString(), bundle);
        }
        for (BundleDescription bundle : sortedMap.values()) {
            addDependencyViaImportPackage(bundle, added, map, entries);
        }

        return entries;
    }

    private Multimap<BundleDescription, AccessRule> retrieveVisiblePackagesFromState(StateHelper helper,
            BundleDescription desc) {
        Multimap<BundleDescription, AccessRule> visiblePackages = LinkedHashMultimap.create();
        if (desc != null) {
            addVisiblePackagesFromState(helper, desc, visiblePackages);
            if (desc.getHost() != null) {
                addVisiblePackagesFromState(helper, (BundleDescription) desc.getHost().getSupplier(), visiblePackages);
            }
        }
        return visiblePackages;
    }

    private void addVisiblePackagesFromState(StateHelper helper, BundleDescription desc,
            Multimap<BundleDescription, AccessRule> visiblePackages) {
        if (desc == null)
            return;
        ExportPackageDescription[] exports = helper.getVisiblePackages(desc, StateHelper.VISIBLE_INCLUDE_EE_PACKAGES);
        for (ExportPackageDescription export : exports) {
            BundleDescription exporter = export.getExporter();
            if (exporter == null)
                continue;
            visiblePackages.put(exporter, getRule(helper, desc, export));
        }
    }

    private AccessRule getRule(StateHelper helper, BundleDescription desc, ExportPackageDescription export) {
        boolean discouraged = helper.getAccessCode(desc, export) == StateHelper.ACCESS_DISCOURAGED;
        String name = export.getName();
        String path = (name.equals(".")) ? "*" : name.replace('.', '/') + "/*";
        return new DefaultAccessRule(path, discouraged);
    }

    protected void addDependencyViaImportPackage(BundleDescription desc, Collection<BundleDescription> added,
            Multimap<BundleDescription, AccessRule> map, Collection<DependencyEntry> entries) {
        if (desc == null || !added.add(desc)) {
            return;
        }

        addPlugin(desc, true, map, entries);

        if (desc.getContainingState() != null) {
            BundleDescription[] fragments = desc.getFragments();
            for (BundleDescription fragment : fragments) {
                if (fragment.isResolved()) {
                    addDependencyViaImportPackage(fragment, added, map, entries);
                }
            }
        }
    }

    private void addDependency(BundleDescription desc, Collection<BundleDescription> added,
            Multimap<BundleDescription, AccessRule> map, Collection<DependencyEntry> entries) {
        addDependency(desc, added, map, entries, true);
    }

    private void addDependency(BundleDescription desc, Collection<BundleDescription> added,
            Multimap<BundleDescription, AccessRule> map, Collection<DependencyEntry> entries, boolean useInclusion) {
        if (desc == null || !added.add(desc))
            return;

        BundleDescription[] fragments = desc.getFragments();

        addPlugin(desc, useInclusion, map, entries);

        // add fragments that are not patches after the host
        for (BundleDescription fragment : fragments) {
            if (fragment.isResolved()) {
                addDependency(fragment, added, map, entries, useInclusion);
            }
        }

        BundleSpecification[] required = desc.getRequiredBundles();
        for (BundleSpecification required1 : required) {
            addDependency((BundleDescription) required1.getSupplier(), added, map, entries, useInclusion);
        }
    }

    private void addPlugin(BundleDescription desc, boolean useInclusions, Multimap<BundleDescription, AccessRule> map,
            Collection<DependencyEntry> entries) {
        Collection<AccessRule> rules = useInclusions ? getInclusions(map, desc) : null;
        DependencyEntry entry = new DependencyEntry(desc, rules);
        if (!entries.contains(entry)) {
            entries.add(entry);
        }
    }

    private Collection<AccessRule> getInclusions(Multimap<BundleDescription, AccessRule> map, BundleDescription desc) {
        Collection<AccessRule> rules;

        if (desc.getHost() != null) {
            rules = map.get((BundleDescription) desc.getHost().getSupplier());
        } else {
            rules = map.get(desc);
        }

        return rules != null ? rules : new ArrayList<>();
    }

    private void addHostPlugin(HostSpecification hostSpec, Collection<BundleDescription> added,
            Multimap<BundleDescription, AccessRule> map, Collection<DependencyEntry> entries) {
        BaseDescription desc = hostSpec.getSupplier();

        if (desc instanceof BundleDescription) {
            BundleDescription host = (BundleDescription) desc;

            // add host plug-in
            if (added.add(host)) {
                addPlugin(host, false, map, entries);
                BundleSpecification[] required = host.getRequiredBundles();
                for (BundleSpecification required1 : required) {
                    addDependency((BundleDescription) required1.getSupplier(), added, map, entries);
                }

                // add Import-Package
                ImportPackageSpecification[] imports = host.getImportPackages();
                for (ImportPackageSpecification import1 : imports) {
                    BaseDescription supplier = import1.getSupplier();
                    if (supplier instanceof ExportPackageDescription) {
                        addDependencyViaImportPackage(((ExportPackageDescription) supplier).getExporter(), added, map,
                                entries);
                    }
                }
            }
        }
    }

    /**
     * Although totally not obvious from the specification text, section 3.15 "Extension Bundles" of
     * OSGi Core Spec apparently says that framework extension bundles can export additional
     * packaged of the underlying JRE. More specific explanation is provided in [1] and I verified
     * that at least Equinox 3.7.1 does indeed behave like described.
     * <p/>
     * There does not seem to be a way to tell which packages exported by a framework extension
     * bundle are supposed to come from JRE and which from the bundle itself, so returned classpath
     * access rules include all packages exported by the framework extension bundles.
     * 
     * [1] http://blog.meschberger.ch/2008/10/osgi-bundles-require-classes-from.html
     */
    public List<AccessRule> computeBootClasspathExtraAccessRules(StateHelper helper, BundleDescription desc) {
        List<AccessRule> result = new ArrayList<>();
        ExportPackageDescription[] exports = helper.getVisiblePackages(desc);
        for (ExportPackageDescription export : exports) {
            BundleDescription host = export.getExporter();
            BaseDescription fragment = ((ExportPackageDescriptionImpl) export).getFragmentDeclaration();
            if (host.getBundleId() == 0 && fragment != null && isFrameworkExtension(fragment.getSupplier())) {
                result.add(getRule(helper, host, export));
            }
        }
        return result;
    }

    private boolean isFrameworkExtension(BundleDescription bundle) {
        OsgiManifest mf = manifestReader.loadManifest(new File(bundle.getLocation()));
        ManifestElement[] elements = mf.getManifestElements(Constants.FRAGMENT_HOST);
        return elements.length == 1
                && Constants.EXTENSION_FRAMEWORK.equals(elements[0].getDirective(Constants.EXTENSION_DIRECTIVE));
    }
}
