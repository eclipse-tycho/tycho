/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.resource.Capability;

/**
 * Helper class that computes compile dependencies of a bundle project.
 * 
 * Code below is copy&paste of org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer
 * adopted to work outside of Eclipse runtime.
 * 
 * Note that some functionality, namely SecondaryDependencies, ExtraClasspathEntries and
 * isPatchFragment, has been removed due to time constraints.
 */
public class DependencyComputer {

    public static class DependencyEntry {
        public final ModuleRevision module;
        public final Collection<AccessRule> rules;

        public DependencyEntry(ModuleRevision module, Collection<AccessRule> rules) {
            this.module = module;
            this.rules = rules;
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, rules);
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
            return Objects.equals(this.module, other.module) && Objects.equals(this.rules, other.rules);
        }
    }

    private static final class VisiblePackages {
        private final Map<ModuleRevision, Set<AccessRule>> visiblePackages = new HashMap<>();
        private final ModuleRevision consumerHost;

        public VisiblePackages(ModuleRevision consumerHost) {
            this.consumerHost = consumerHost;
        }

        public void add(ModuleCapability packageCapability) {
            AccessRule rule = createRule(consumerHost, packageCapability);
            visiblePackages.computeIfAbsent(packageCapability.getResource(), m -> new LinkedHashSet<>()).add(rule);
        }

        public Set<AccessRule> getInclusions(ModuleRevision module) {
            Set<AccessRule> rules = visiblePackages.get(module);
            return rules != null ? Collections.unmodifiableSet(rules) : Collections.emptySet();
        }

        public Set<ModuleRevision> getParticipatingModules() {
            return Collections.unmodifiableSet(visiblePackages.keySet());
        }
    }

    private final ModuleContainer container;

    /**
     * Creates a new DependencyComputer associated to the given {@link ModuleContainer}.
     * <p>
     * Because this DependencyComputer caches the {@link ModuleWire ModuleWires} of a
     * {@link ModuleWiring} but these wires may change between modules this DependencyComputer will
     * only compute dependencies of {@link ModuleRevision ModuleRevisions} from the given container.
     * </p>
     * 
     * @param container
     */
    public DependencyComputer(ModuleContainer container) {
        this.container = container;
    }

    /**
     * Computes and returns the List of {@link DependencyEntry dependencies} of the given
     * {@link ModuleRevision}.
     * <p>
     * The given {@code ModuleRevision} must be contained in the {@link ModuleContainer} associated
     * to this {@code DependencyComputer} in order to ensure cache integrity.
     * </p>
     * 
     * @param module
     *            the ModuleRevision whose dependencies are computed
     * @return the list of dependencies of the module
     * @see #DependencyComputer(ModuleContainer)
     */
    public List<DependencyEntry> computeDependencies(ModuleRevision module) {
        if (module == null) {
            return Collections.emptyList();
        }
        assertAssociatedModuleContainer(module.getRevisions().getModule().getContainer());

        VisiblePackages visiblePackages = getPackagesInternal(module);
        Set<ModuleRevision> added = new HashSet<>();

        // to avoid cycles, e.g. when a bundle imports a package it exports
        added.add(module);

        List<DependencyEntry> entries = new ArrayList<>();
        getFragmentHost(module).ifPresent(host -> addHostPlugin(host, added, visiblePackages, entries));
        getRequiredBundles(module).forEach(required -> addDependency(required, added, visiblePackages, entries));

        // add Import-Package
        // sort by symbolicName_version to get a consistent order
        Map<String, ModuleRevision> resolvedImportPackages = new TreeMap<>();
        for (ModuleRevision bundle : visiblePackages.getParticipatingModules()) {
            resolvedImportPackages.put(bundle.getSymbolicName(), bundle);
        }
        for (ModuleRevision bundle : resolvedImportPackages.values()) {
            addDependencyViaImportPackage(bundle, added, visiblePackages, entries);
        }

        return entries;
    }

    private Collection<ModuleRevision> getRequiredBundles(ModuleRevision module) {
        if (module == null) {
            return Collections.emptyList();
        }
        return getRequiredBundleModuleWires(module.getWiring()).stream().map(ModuleWire::getProvider)
                .collect(Collectors.toList());
    }

    private Optional<ModuleRevision> getFragmentHost(ModuleRevision bundleRevision) {
        return getRequiredHostModuleWires(bundleRevision.getWiring()).stream().map(ModuleWire::getProvider).findAny();
    }

    class PackageSource {
        public final ModuleCapability cap;
        public final ModuleWire wire;

        PackageSource(ModuleCapability cap, ModuleWire wire) {
            this.cap = cap;
            this.wire = wire;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof PackageSource) {
                return Objects.equals(cap, ((PackageSource) o).cap)
                        && Objects.equals(wire.getProvider(), ((PackageSource) o).wire.getProvider());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return cap.hashCode() ^ wire.getProvider().hashCode();
        }
    }

    private VisiblePackages getPackagesInternal(ModuleRevision module) {
        Map<String, Set<PackageSource>> sources = getPackagesInternal0(module.getWiring(), null);
        VisiblePackages res = new VisiblePackages(getFragmentHost(module).orElse(module));
        sources.values().stream() //
                .flatMap(Set::stream) //
                .map(p -> p.cap) //
                .forEach(res::add);
        return res;
    }

    // This part of resolution is copied and adapted from EquinoxCommandProvider `getPackages` implementation
    private Map<String, Set<PackageSource>> getPackagesInternal0(ModuleWiring wiring,
            Map<ModuleWiring, Map<String, Set<PackageSource>>> allSources) {
        if (allSources == null) {
            allSources = new HashMap<>();
        }
        Map<String, Set<PackageSource>> packages = allSources.get(wiring);
        if (packages != null) {
            return packages;
        }
        packages = new TreeMap<>();
        allSources.put(wiring, packages);

        Set<String> importedPackageNames = new HashSet<>();
        populateFromWiring(wiring, allSources, packages, importedPackageNames);
        for (ModuleWire fragmentWire : getRequiredHostModuleWires(wiring)) {
            populateFromWiring(fragmentWire.getProviderWiring(), allSources, packages, importedPackageNames);
        }
        return packages;
    }

    private void populateFromWiring(ModuleWiring wiring, Map<ModuleWiring, Map<String, Set<PackageSource>>> allSources,
            Map<String, Set<PackageSource>> packages, Set<String> importedPackageNames) {
        // first get the imported packages
        for (ModuleWire packageWire : getRequiredPackageModuleWires(wiring)) {
            String packageName = getPackageName(packageWire.getCapability());
            importedPackageNames.add(packageName);
            addAggregatePackageSource(packageWire.getCapability(), packageName, packageWire, packages, allSources);
        }
        // now get packages from required bundles
        for (ModuleWire requiredWire : getRequiredBundleModuleWires(wiring)) {
            getRequiredBundlePackages(requiredWire, importedPackageNames, packages, allSources);
        }
    }

    private void addAggregatePackageSource(ModuleCapability packageCap, String packageName, ModuleWire wire,
            Map<String, Set<PackageSource>> packages, Map<ModuleWiring, Map<String, Set<PackageSource>>> allSources) {
        Set<PackageSource> packageSources = packages.computeIfAbsent(packageName, p -> new LinkedHashSet<>());
        packageSources.add(new PackageSource(packageCap, wire));
        // Tycho-specific: Case of split package with fragment, not part of `getPackages` console command but necessary for Tycho
        for (ModuleWire fragmentWire : getProvidedHostModuleWires(packageCap.getResource().getWiring())) {
            for (ModuleCapability fragmentExport : fragmentWire.getRequirer()
                    .getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
                if (getPackageName(fragmentExport).equals(getPackageName(packageCap))) {
                    packageSources.add(new PackageSource(fragmentExport, wire));
                }
            }
        }
        // source may be a split package aggregate
        Set<PackageSource> providerSource = getPackagesInternal0(wire.getProviderWiring(), allSources).get(packageName);
        if (providerSource != null) {
            packageSources.addAll(providerSource);
        }
    }

    private void getRequiredBundlePackages(ModuleWire requiredWire, Set<String> importedPackageNames,
            Map<String, Set<PackageSource>> packages, Map<ModuleWiring, Map<String, Set<PackageSource>>> allSources) {
        ModuleWiring providerWiring = requiredWire.getProviderWiring();
        for (ModuleCapability packageCapability : providerWiring
                .getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
            String packageName = getPackageName(packageCapability);
            // if imported then packages from required bundles do not get added
            if (!importedPackageNames.contains(packageName)) {
                addAggregatePackageSource(packageCapability, packageName, requiredWire, packages, allSources);
            }
        }

        // get the declared packages
        Set<String> declaredPackageNames = new HashSet<>();
        for (BundleCapability declaredPackage : providerWiring.getRevision()
                .getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
            declaredPackageNames.add(getPackageName(declaredPackage));
        }
        // and from attached fragments
        for (BundleWire fragmentWire : getProvidedHostWires(providerWiring)) {
            for (BundleCapability declaredPackage : fragmentWire.getRequirer()
                    .getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
                declaredPackageNames.add(getPackageName(declaredPackage));
            }
        }

        for (ModuleWire packageWire : getRequiredPackageModuleWires(providerWiring)) {
            String packageName = getPackageName(packageWire.getCapability());
            if (!importedPackageNames.contains(packageName) && declaredPackageNames.contains(packageName)) {
                // if the package is a declared capability AND the wiring imports the package
                // then it is substituted
                addAggregatePackageSource(packageWire.getCapability(), packageName, packageWire, packages, allSources);
            }
        }

        // now get packages from re-exported requires of the required bundle
        for (ModuleWire providerBundleWire : getRequiredBundleModuleWires(providerWiring)) {
            String visibilityDirective = providerBundleWire.getRequirement().getDirectives()
                    .get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
            if (BundleNamespace.VISIBILITY_REEXPORT.equals(visibilityDirective)) {
                getRequiredBundlePackages(providerBundleWire, importedPackageNames, packages, allSources);
            }
        }
    }

    private static AccessRule createRule(ModuleRevision consumer, Capability export) {
        String name = getPackageName(export);
        String path = (name.equals(".")) ? "*" : name.replace('.', '/') + "/*";
        return new DefaultAccessRule(path, isDiscouragedAccess(consumer, export));
    }

    private static String getPackageName(Capability capability) {
        return (String) capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
    }

    protected void addDependencyViaImportPackage(ModuleRevision module, Collection<ModuleRevision> added,
            VisiblePackages visiblePackages, Collection<DependencyEntry> entries) {
        if (module == null || !added.add(module)) {
            return;
        }

        addPlugin(module, true, visiblePackages, entries);

        for (ModuleRevision fragment : getFragments(module)) {
            addDependencyViaImportPackage(fragment, added, visiblePackages, entries);
        }
    }

    private Collection<ModuleRevision> getFragments(ModuleRevision host) {
        if (host == null /* || !isExtensibleAPI(host) */) {
            return Collections.emptyList();
        }

        return getProvidedHostModuleWires(host.getWiring()).stream().map(ModuleWire::getRequirer)
                .collect(Collectors.toList());
    }

    private void addDependency(ModuleRevision desc, Collection<ModuleRevision> added, VisiblePackages visiblePackages,
            Collection<DependencyEntry> entries) {
        addDependency(desc, added, visiblePackages, entries, true);
    }

    private void addDependency(ModuleRevision desc, Collection<ModuleRevision> added, VisiblePackages visiblePackages,
            Collection<DependencyEntry> entries, boolean useInclusion) {
        if (desc == null || !added.add(desc))
            return;

        addPlugin(desc, useInclusion, visiblePackages, entries);

        // add fragments that are not patches after the host
        for (ModuleRevision fragment : getFragments(desc)) {
            addDependency(fragment, added, visiblePackages, entries, useInclusion);
        }

        for (ModuleRevision required : getRequiredBundles(desc)) {
            addDependency(required, added, visiblePackages, entries, useInclusion);
        }
    }

    private void addPlugin(ModuleRevision module, boolean useInclusions, VisiblePackages visiblePackages,
            Collection<DependencyEntry> entries) {
        Set<AccessRule> rules;
        if (useInclusions) {
            rules = visiblePackages.getInclusions(module);
            Optional<ModuleRevision> host = getFragmentHost(module);
            if (host.isPresent()) {
                rules = new HashSet<>(rules);
                rules.addAll(visiblePackages.getInclusions(host.get()));
            }
        } else {
            rules = null;
        }
        DependencyEntry entry = new DependencyEntry(module, rules);
        if (!entries.contains(entry)) {
            entries.add(entry);
        }
    }

    private void addHostPlugin(ModuleRevision host, Collection<ModuleRevision> added, VisiblePackages visiblePackages,
            Collection<DependencyEntry> entries) {
        if (host == null) {
            return;
        }
        // add host plug-in
        if (added.add(host)) {
            addPlugin(host, false, visiblePackages, entries);
            for (ModuleRevision required : getRequiredBundles(host)) {
                addDependency(required, added, visiblePackages, entries);
            }

            // add Import-Package
            getRequiredPackageModuleWires(host.getWiring()).stream().map(ModuleWire::getProvider)
                    .forEach(provider -> addDependencyViaImportPackage(provider, added, visiblePackages, entries));
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
    public List<AccessRule> computeBootClasspathExtraAccessRules(ModuleContainer container) {
        assertAssociatedModuleContainer(container);
        ModuleRevision systemBundle = container.getModule(Constants.SYSTEM_BUNDLE_ID).getCurrentRevision();
        return getProvidedHostModuleWires(systemBundle.getWiring()).stream().map(ModuleWire::getRequirer).flatMap(
                systemFragment -> systemFragment.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE).stream())
                .map(packageExport -> createRule(systemBundle, packageExport)).collect(Collectors.toList());
    }

    public static boolean isDiscouragedAccess(BundleRevision bundle, Capability export) {
        if (Boolean.parseBoolean(export.getDirectives().get("x-internal"))) {
            return true;
        }
        String allFriends = export.getDirectives().get("x-friends");
        if (allFriends != null) {
            return Arrays.stream(allFriends.split(",")).map(String::trim).noneMatch(bundle.getSymbolicName()::equals);
        }
        return false;
    }

    private void assertAssociatedModuleContainer(ModuleContainer container) {
        if (container != this.container) {
            throw new IllegalStateException("ModuleRevision not from the associated ModuleContainer");
        }
    }

    // --- caching of wiries ---

    private final Map<ModuleWiring, List<ModuleWire>> wiringsRequiredHostModuleWiries = new HashMap<>();
    private final Map<ModuleWiring, List<ModuleWire>> wiringsRequiredBundleModuleWiries = new HashMap<>();
    private final Map<ModuleWiring, List<ModuleWire>> wiringsRequiredPackageModuleWiries = new HashMap<>();

    private final Map<ModuleWiring, List<ModuleWire>> wiringsProvidedHostModuleWiries = new HashMap<>();
    private final Map<ModuleWiring, List<BundleWire>> wiringsProvidedHostWiries = new HashMap<>();

    private List<ModuleWire> getRequiredHostModuleWires(ModuleWiring wiring) {
        return wiringsRequiredHostModuleWiries.computeIfAbsent(wiring,
                w -> w.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE));
    }

    private List<ModuleWire> getRequiredBundleModuleWires(ModuleWiring wiring) {
        return wiringsRequiredBundleModuleWiries.computeIfAbsent(wiring,
                w -> w.getRequiredModuleWires(BundleNamespace.BUNDLE_NAMESPACE));
    }

    private List<ModuleWire> getRequiredPackageModuleWires(ModuleWiring wiring) {
        return wiringsRequiredPackageModuleWiries.computeIfAbsent(wiring,
                w -> w.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE));
    }

    private List<ModuleWire> getProvidedHostModuleWires(ModuleWiring wiring) {
        return wiringsProvidedHostModuleWiries.computeIfAbsent(wiring,
                w -> w.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE));
    }

    private List<BundleWire> getProvidedHostWires(ModuleWiring wiring) {
        return wiringsProvidedHostWiries.computeIfAbsent(wiring, w -> w.getProvidedWires(HostNamespace.HOST_NAMESPACE));
    }
}
