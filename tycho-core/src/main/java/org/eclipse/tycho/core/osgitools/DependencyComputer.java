/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.codehaus.plexus.component.annotations.Component;
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
@Component(role = DependencyComputer.class)
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

    private final class VisiblePackages {
        private final Map<ModuleRevision, Set<AccessRule>> visiblePackages = new HashMap<>();
        private final ModuleRevision consumerHost;

        public VisiblePackages(ModuleRevision consumer) {
            this.consumerHost = getFragmentHost(consumer).orElse(consumer);
        }

        public void add(ModuleCapability packageCapability) {
            AccessRule rule = createRule(consumerHost, packageCapability);
            visiblePackages.computeIfAbsent(packageCapability.getResource(), m -> new LinkedHashSet<>()).add(rule);
        }

        public Collection<AccessRule> getInclusions(ModuleRevision module) {
            Set<AccessRule> rules = visiblePackages.getOrDefault(module, Collections.emptySet());
            Optional<ModuleRevision> host = getFragmentHost(module);
            if (host.isPresent()) {
                Set<AccessRule> hostRules = visiblePackages.getOrDefault(host.get(), Collections.emptySet());
                rules = new HashSet<>(rules);
                rules.addAll(hostRules);
            }
            return Collections.unmodifiableSet(rules);
        }

        public Collection<ModuleRevision> getParticipatingModules() {
            return Collections.unmodifiableSet(visiblePackages.keySet());
        }
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

        VisiblePackages visiblePackages = getPackagesInternal(module);
        Set<ModuleRevision> added = new HashSet<>();

        // to avoid cycles, e.g. when a bundle imports a package it exports
        added.add(module);

        Set<DependencyEntry> entries = new LinkedHashSet<>();
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

        return new ArrayList<>(entries);
    }

    private Collection<ModuleRevision> getRequiredBundles(ModuleRevision module) {
        if (module == null) {
            return Collections.emptyList();
        }
        return module.getWiring().getRequiredModuleWires(BundleNamespace.BUNDLE_NAMESPACE).stream()
                .map(ModuleWire::getProvider).collect(Collectors.toList());
    }

    private static Optional<ModuleRevision> getFragmentHost(ModuleRevision bundleRevision) {
        return bundleRevision.getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE).stream()
                .map(ModuleWire::getProvider).findAny();
    }

    private VisiblePackages getPackagesInternal(ModuleRevision module) {
        Map<String, Set<ModuleCapability>> sources = getPackagesInternal0(module.getWiring(), new HashMap<>());
        VisiblePackages res = new VisiblePackages(module);
        sources.values().stream().flatMap(Set::stream).forEach(res::add);
        return res;
    }

    // This part of resolution is copied and adapted from EquinoxCommandProvider `getPackages` implementation
    private Map<String, Set<ModuleCapability>> getPackagesInternal0(ModuleWiring wiring,
            Map<ModuleWiring, Map<String, Set<ModuleCapability>>> allSources) {

        Map<String, Set<ModuleCapability>> packages = allSources.get(wiring);
        if (packages != null) {
            return packages;
        }
        packages = new TreeMap<>();
        allSources.put(wiring, packages);

        Set<String> importedPackageNames = new HashSet<>();
        populateFromWiring(wiring, allSources, packages, importedPackageNames);
        for (ModuleWire fragmentWire : wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE)) {
            populateFromWiring(fragmentWire.getProviderWiring(), allSources, packages, importedPackageNames);
        }
        return packages;
    }

    private void populateFromWiring(ModuleWiring wiring,
            Map<ModuleWiring, Map<String, Set<ModuleCapability>>> allSources,
            Map<String, Set<ModuleCapability>> packages, Set<String> importedPackageNames) {

        // first get the imported packages
        for (ModuleWire packageWire : wiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE)) {
            String packageName = getPackageName(packageWire.getCapability());
            importedPackageNames.add(packageName);
            addAggregatePackageSource(packageWire.getCapability(), packageName, packageWire, packages, allSources);
        }

        // now get packages from its required bundles and all accessible bundles through visibility:reexport 
        for (ModuleWire requiredWire : getRequiredAndAllAccessibleModuleWires(wiring)) {
            getRequiredBundlePackages(requiredWire, importedPackageNames, packages, allSources);
        }
    }

    /**
     * For a given moduleWiring, retrieve the list of all requiredModuleWires, including the
     * moduleWires that are accessible with a visibility:reexport
     */
    private Collection<ModuleWire> getRequiredAndAllAccessibleModuleWires(ModuleWiring wiring) {
        Collection<ModuleWire> requiredAndReexportedWires = new LinkedHashSet<>();
        LinkedList<ModuleWire> toVisitWires = new LinkedList<>();
        toVisitWires.addAll(wiring.getRequiredModuleWires(BundleNamespace.BUNDLE_NAMESPACE));

        while (!toVisitWires.isEmpty()) {
            ModuleWire moduleWire = toVisitWires.removeFirst();
            if (requiredAndReexportedWires.add(moduleWire)) {
                ModuleWiring providerWiring = moduleWire.getProviderWiring();
                toVisitWires.addAll(getRequiredModuleWiresWithVisibilityReexport(providerWiring));
            }
        }
        return requiredAndReexportedWires;
    }

    /**
     * For a module, retrieve the list of required modules with a visibility:reexport
     */
    private Collection<ModuleWire> getRequiredModuleWiresWithVisibilityReexport(ModuleWiring wiring) {
        return wiring.getRequiredModuleWires(BundleNamespace.BUNDLE_NAMESPACE).stream()
                .filter(DependencyComputer::hasVisibilityReexport).collect(Collectors.toList());
    }

    private void addAggregatePackageSource(ModuleCapability packageCap, String packageName, ModuleWire wire,
            Map<String, Set<ModuleCapability>> packages,
            Map<ModuleWiring, Map<String, Set<ModuleCapability>>> allSources) {
        Set<ModuleCapability> packageSources = packages.computeIfAbsent(packageName, p -> new LinkedHashSet<>());
        packageSources.add(packageCap);
        // Tycho-specific: Case of split package with fragment, not part of `getPackages` console command but necessary for Tycho
        for (ModuleWire fragmentWire : packageCap.getResource().getWiring()
                .getProvidedModuleWires(HostNamespace.HOST_NAMESPACE)) {
            for (ModuleCapability fragmentExport : fragmentWire.getRequirer()
                    .getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
                if (getPackageName(fragmentExport).equals(getPackageName(packageCap))) {
                    packageSources.add(fragmentExport);
                }
            }
        }
        // source may be a split package aggregate
        Set<ModuleCapability> providerSource = getPackagesInternal0(wire.getProviderWiring(), allSources)
                .get(packageName);
        if (providerSource != null) {
            packageSources.addAll(providerSource);
        }
    }

    private void getRequiredBundlePackages(ModuleWire requiredWire, Set<String> importedPackageNames,
            Map<String, Set<ModuleCapability>> packages,
            Map<ModuleWiring, Map<String, Set<ModuleCapability>>> allSources) {
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
        for (BundleWire fragmentWire : providerWiring.getProvidedWires(HostNamespace.HOST_NAMESPACE)) {
            for (BundleCapability declaredPackage : fragmentWire.getRequirer()
                    .getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
                declaredPackageNames.add(getPackageName(declaredPackage));
            }
        }

        for (ModuleWire packageWire : providerWiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE)) {
            String packageName = getPackageName(packageWire.getCapability());
            if (!importedPackageNames.contains(packageName) && declaredPackageNames.contains(packageName)) {
                // if the package is a declared capability AND the wiring imports the package
                // then it is substituted
                addAggregatePackageSource(packageWire.getCapability(), packageName, packageWire, packages, allSources);
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

    private void addDependencyViaImportPackage(ModuleRevision module, Collection<ModuleRevision> added,
            VisiblePackages visiblePackages, Set<DependencyEntry> entries) {
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
        return host.getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE).stream()
                .map(ModuleWire::getRequirer).collect(Collectors.toList());
    }

    private void addDependency(ModuleRevision desc, Collection<ModuleRevision> added, VisiblePackages visiblePackages,
            Set<DependencyEntry> entries) {
        addDependency(desc, added, visiblePackages, entries, true);
    }

    private void addDependency(ModuleRevision desc, Collection<ModuleRevision> added, VisiblePackages visiblePackages,
            Set<DependencyEntry> entries, boolean useInclusion) {
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
            Set<DependencyEntry> entries) {
        Collection<AccessRule> rules = useInclusions ? visiblePackages.getInclusions(module) : null;
        DependencyEntry entry = new DependencyEntry(module, rules);
        entries.add(entry);
    }

    private void addHostPlugin(ModuleRevision host, Set<ModuleRevision> added, VisiblePackages visiblePackages,
            Set<DependencyEntry> entries) {
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
            host.getWiring().getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE).stream()
                    .map(ModuleWire::getProvider)
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
     * [1] https://blog.meschberger.ch/2008/10/osgi-bundles-require-classes-from.html
     */

    public List<AccessRule> computeBootClasspathExtraAccessRules(ModuleContainer container) {
        ModuleRevision systemBundle = container.getModule(Constants.SYSTEM_BUNDLE_ID).getCurrentRevision();
        return systemBundle.getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE).stream()
                .map(ModuleWire::getRequirer)
                .flatMap(systemFragment -> systemFragment.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE)
                        .stream())
                .map(packageExport -> createRule(systemBundle, packageExport)).collect(Collectors.toList());
    }

    private static boolean isDiscouragedAccess(BundleRevision bundle, Capability export) {
        if (Boolean.parseBoolean(export.getDirectives().get("x-internal"))) {
            return true;
        }
        String allFriends = export.getDirectives().get("x-friends");
        if (allFriends != null) {
            return Arrays.stream(allFriends.split(",")).map(String::trim).noneMatch(bundle.getSymbolicName()::equals);
        }
        return false;
    }

    private static boolean hasVisibilityReexport(ModuleWire moduleWire) {
        String visibilityDirective = moduleWire.getRequirement().getDirectives()
                .get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
        return BundleNamespace.VISIBILITY_REEXPORT.equals(visibilityDirective);
    }

}
