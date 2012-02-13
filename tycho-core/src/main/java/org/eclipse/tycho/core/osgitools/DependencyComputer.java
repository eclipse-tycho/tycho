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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        public final List<AccessRule> rules;

        public DependencyEntry(BundleDescription desc, List<AccessRule> rules) {
            this.desc = desc;
            this.rules = rules;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DependencyEntry)) {
                return false;
            }
            DependencyEntry other = (DependencyEntry) obj;
            return desc.equals(other.desc) && rules.equals(other.rules);
        }
    }

    public List<DependencyEntry> computeDependencies(StateHelper helper, BundleDescription desc) {
        ArrayList<DependencyEntry> entries = new ArrayList<DependencyEntry>();

        if (desc == null)
            return entries;

        Map<BundleDescription, ArrayList<AccessRule>> map = retrieveVisiblePackagesFromState(helper, desc);

        HashSet<BundleDescription> added = new HashSet<BundleDescription>();

        // to avoid cycles, e.g. when a bundle imports a package it exports
        added.add(desc);

        HostSpecification host = desc.getHost();
        if (host != null) {
            addHostPlugin(host, added, map, entries);
        }

        // add dependencies
        BundleSpecification[] required = desc.getRequiredBundles();
        for (int i = 0; i < required.length; i++) {
            addDependency((BundleDescription) required[i].getSupplier(), added, map, entries);
        }

//		addSecondaryDependencies(desc, added, entries);

        // add Import-Package
        // sort by symbolicName_version to get a consistent order
        Map<String, BundleDescription> sortedMap = new TreeMap<String, BundleDescription>();
        for (BundleDescription bundle : map.keySet()) {
            sortedMap.put(bundle.toString(), bundle);
        }
        for (BundleDescription bundle : sortedMap.values()) {
            addDependencyViaImportPackage(bundle, added, map, entries);
        }

//		addExtraClasspathEntries(added, entries);

//		for (int i = 0; i < entries.size(); i++) {
//			System.err.println(i + "\t" + entries.get(i).desc);
//		}

        return entries;
    }

    private Map<BundleDescription, ArrayList<AccessRule>> retrieveVisiblePackagesFromState(StateHelper helper,
            BundleDescription desc) {
        Map<BundleDescription, ArrayList<AccessRule>> visiblePackages = new HashMap<BundleDescription, ArrayList<AccessRule>>();
        addVisiblePackagesFromState(helper, desc, visiblePackages);
        if (desc.getHost() != null)
            addVisiblePackagesFromState(helper, (BundleDescription) desc.getHost().getSupplier(), visiblePackages);
        return visiblePackages;
    }

    private void addVisiblePackagesFromState(StateHelper helper, BundleDescription desc,
            Map<BundleDescription, ArrayList<AccessRule>> visiblePackages) {
        if (desc == null)
            return;
        ExportPackageDescription[] exports = helper.getVisiblePackages(desc);
        for (int i = 0; i < exports.length; i++) {
            BundleDescription exporter = exports[i].getExporter();
            if (exporter == null)
                continue;
            ArrayList<AccessRule> list = visiblePackages.get(exporter);
            if (list == null)
                list = new ArrayList<AccessRule>();
            AccessRule rule = getRule(helper, desc, exports[i]);
            if (!list.contains(rule))
                list.add(rule);
            visiblePackages.put(exporter, list);
        }
    }

    private AccessRule getRule(StateHelper helper, BundleDescription desc, ExportPackageDescription export) {
        boolean discouraged = helper.getAccessCode(desc, export) == StateHelper.ACCESS_DISCOURAGED;
        String name = export.getName();
        String path = (name.equals(".")) ? "*" : name.replaceAll("\\.", "/") + "/*";
        return new DefaultAccessRule(path, discouraged);
    }

    protected void addDependencyViaImportPackage(BundleDescription desc, HashSet<BundleDescription> added,
            Map<BundleDescription, ArrayList<AccessRule>> map, ArrayList<DependencyEntry> entries) {
        if (desc == null || !added.add(desc))
            return;

        addPlugin(desc, true, map, entries);

        if (hasExtensibleAPI(desc) && desc.getContainingState() != null) {
            BundleDescription[] fragments = desc.getFragments();
            for (int i = 0; i < fragments.length; i++) {
                if (fragments[i].isResolved())
                    addDependencyViaImportPackage(fragments[i], added, map, entries);
            }
        }
    }

    private void addDependency(BundleDescription desc, HashSet<BundleDescription> added,
            Map<BundleDescription, ArrayList<AccessRule>> map, ArrayList<DependencyEntry> entries) {
        addDependency(desc, added, map, entries, true);
    }

    private void addDependency(BundleDescription desc, HashSet<BundleDescription> added,
            Map<BundleDescription, ArrayList<AccessRule>> map, ArrayList<DependencyEntry> entries, boolean useInclusion) {
        if (desc == null || !added.add(desc))
            return;

        BundleDescription[] fragments = hasExtensibleAPI(desc) ? desc.getFragments() : new BundleDescription[0];

        // add fragment patches before host
        for (int i = 0; i < fragments.length; i++) {
            if (fragments[i].isResolved() && isPatchFragment(fragments[i])) {
                addDependency(fragments[i], added, map, entries, useInclusion);
            }
        }

        addPlugin(desc, useInclusion, map, entries);

        // add fragments that are not patches after the host
        for (int i = 0; i < fragments.length; i++) {
            if (fragments[i].isResolved() && !isPatchFragment(fragments[i])) {
                addDependency(fragments[i], added, map, entries, useInclusion);
            }
        }

        BundleSpecification[] required = desc.getRequiredBundles();
        for (int i = 0; i < required.length; i++) {
            addDependency((BundleDescription) required[i].getSupplier(), added, map, entries, useInclusion);
        }
    }

    private boolean isPatchFragment(BundleDescription bundleDescription) {
        return false; // TODO
    }

    private boolean addPlugin(BundleDescription desc, boolean useInclusions,
            Map<BundleDescription, ArrayList<AccessRule>> map, ArrayList<DependencyEntry> entries) {
        if (EquinoxResolver.SYSTEM_BUNDLE_SYMBOLIC_NAME.equals(desc.getSymbolicName())) {
            return false;
        }
        List<AccessRule> rules = useInclusions ? getInclusions(map, desc) : null;
        DependencyEntry entry = new DependencyEntry(desc, rules);
        if (!entries.contains(entry))
            entries.add(entry);
        return true;
    }

    private List<AccessRule> getInclusions(Map<BundleDescription, ArrayList<AccessRule>> map, BundleDescription desc) {
        ArrayList<AccessRule> rules;

        if (desc.getHost() != null)
            rules = map.get((BundleDescription) desc.getHost().getSupplier());
        else
            rules = map.get(desc);

        return rules != null ? rules : new ArrayList<AccessRule>();
    }

    private void addHostPlugin(HostSpecification hostSpec, HashSet<BundleDescription> added,
            Map<BundleDescription, ArrayList<AccessRule>> map, ArrayList<DependencyEntry> entries) {
        BaseDescription desc = hostSpec.getSupplier();

        if (desc instanceof BundleDescription) {
            BundleDescription host = (BundleDescription) desc;

            // add host plug-in
            if (added.add(host) && addPlugin(host, false, map, entries)) {
                BundleSpecification[] required = host.getRequiredBundles();
                for (int i = 0; i < required.length; i++) {
                    addDependency((BundleDescription) required[i].getSupplier(), added, map, entries);
                }

                // add Import-Package
                ImportPackageSpecification[] imports = host.getImportPackages();
                for (int i = 0; i < imports.length; i++) {
                    BaseDescription supplier = imports[i].getSupplier();
                    if (supplier instanceof ExportPackageDescription) {
                        addDependencyViaImportPackage(((ExportPackageDescription) supplier).getExporter(), added, map,
                                entries);
                    }
                }
            }
        }
    }

    private boolean hasExtensibleAPI(BundleDescription desc) {
        // TODO re-enable Eclipse-ExtensibleAPI
        return true; //"true".equals(state.getManifestAttribute(desc, "Eclipse-ExtensibleAPI"));
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
        List<AccessRule> result = new ArrayList<AccessRule>();
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
        if (elements.length == 1) {
            if (Constants.EXTENSION_FRAMEWORK.equals(elements[0].getDirective(Constants.EXTENSION_DIRECTIVE))) {
                return true;
            }
        }
        return false;
    }
}
