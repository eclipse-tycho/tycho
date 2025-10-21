/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    Sonatype Inc. - adopted to work outside of eclipse workspace
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
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
import java.util.function.Function;

import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ClasspathEntry.AccessRule;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
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
@Named
@Singleton
public class DefaultDependencyComputer implements DependencyComputer {

    public static class DependencyEntry implements DependencyComputer.DependencyEntry {
        public final BundleRevision module;
        private final Collection<AccessRule> rules;
        private ArtifactDescriptor descriptor;

        public DependencyEntry(BundleRevision module, ArtifactDescriptor descriptor, Collection<AccessRule> rules) {
            this.module = Objects.requireNonNull(module);
            this.descriptor = Objects.requireNonNull(descriptor);
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

        @Override
        public BundleRevision getRevision() {
            return module;
        }

        @Override
        public File getLocation() {
            if (module instanceof ModuleRevision moduleRevision) {
                return (File) moduleRevision.getRevisionInfo();
            }
            Bundle bundle = module.getBundle();
            if (bundle != null) {
                return new File(bundle.getLocation());
            }
            return null;
        }

        @Override
        public boolean isSystemBundle() {
            if (module instanceof ModuleRevision moduleRevision) {
                return Constants.SYSTEM_BUNDLE_ID == moduleRevision.getRevisions().getModule().getId();
            }
            Bundle bundle = module.getBundle();
            if (bundle != null) {
                return bundle.getBundleId() == Constants.SYSTEM_BUNDLE_ID;
            }
            return false;
        }

        @Override
        public String getSymbolicName() {
            return getRevision().getSymbolicName();
        }

        @Override
        public Version getVersion() {
            return getRevision().getVersion();
        }

        @Override
        public String toString() {
            return "DependencyEntry [module=" + module + ", rules=" + rules + "]";
        }

        @Override
        public ArtifactDescriptor getArtifactDescriptor() {
            return descriptor;
        }

        @Override
        public Collection<AccessRule> getRules() {
            return rules;
        }
    }

    private static final class VisiblePackages {
        private final Map<BundleRevision, Set<AccessRule>> visiblePackages = new HashMap<>();
        private final BundleRevision consumerHost;

        public VisiblePackages(ModuleRevision consumer) {
            this.consumerHost = getFragmentHost(consumer).orElse(consumer);
        }

        public void add(ModuleCapability packageCapability) {
            AccessRule rule = createRule(consumerHost, packageCapability);
            visiblePackages.computeIfAbsent(packageCapability.getResource(), m -> new LinkedHashSet<>()).add(rule);
        }

        public Collection<AccessRule> getInclusions(BundleRevision module) {
            Set<AccessRule> rules = visiblePackages.getOrDefault(module, Collections.emptySet());
            Optional<BundleRevision> host = getFragmentHost(module);
            if (host.isPresent()) {
                Set<AccessRule> hostRules = visiblePackages.getOrDefault(host.get(), Collections.emptySet());
                rules = new HashSet<>(rules);
                rules.addAll(hostRules);
            }
            return Collections.unmodifiableSet(rules);
        }

        public Collection<BundleRevision> getParticipatingModules() {
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
    @Override
    public List<DependencyComputer.DependencyEntry> computeDependencies(ModuleRevision module,
            Function<BundleRevision, ArtifactDescriptor> descriptorLookup) {
        if (module == null || module.getWiring() == null) {
            return Collections.emptyList();
        }

        VisiblePackages visiblePackages = getPackagesInternal(module);
        Set<BundleRevision> added = new HashSet<>();

        // to avoid cycles, e.g. when a bundle imports a package it exports
        added.add(module);

        Set<DependencyEntry> entries = new LinkedHashSet<>();
        getFragmentHost(module)
                .ifPresent(host -> addHostPlugin(host, added, visiblePackages, entries, descriptorLookup));
        getRequiredBundles(module)
                .forEach(required -> addDependency(required, added, visiblePackages, entries, descriptorLookup));

        // add Import-Package
        // sort by symbolicName_version to get a consistent order
        Map<String, BundleRevision> resolvedImportPackages = new TreeMap<>();
        for (BundleRevision bundle : visiblePackages.getParticipatingModules()) {
            resolvedImportPackages.put(bundle.getSymbolicName() + "_" + bundle.getVersion(), bundle);
        }
        for (BundleRevision bundle : resolvedImportPackages.values()) {
            addDependencyViaImportPackage(bundle, added, visiblePackages, entries, descriptorLookup);
        }

        return new ArrayList<>(entries);
    }

    private List<BundleRevision> getRequiredBundles(BundleRevision module) {
        if (module == null) {
            return Collections.emptyList();
        }
        return module.getWiring().getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE).stream()
                .map(BundleWire::getProvider).toList();
    }

    private static Optional<BundleRevision> getFragmentHost(BundleRevision bundleRevision) {
        return bundleRevision.getWiring().getRequiredWires(HostNamespace.HOST_NAMESPACE).stream()
                .map(BundleWire::getProvider).findAny();
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
                .filter(DefaultDependencyComputer::hasVisibilityReexport).toList();
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

    private static AccessRule createRule(BundleRevision consumer, Capability export) {
        String name = getPackageName(export);
        String path = (name.equals(".")) ? "*" : name.replace('.', '/') + "/*";
        return new DefaultAccessRule(path, isDiscouragedAccess(consumer, export));
    }

    private static String getPackageName(Capability capability) {
        return (String) capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
    }

    private void addDependencyViaImportPackage(BundleRevision module, Collection<BundleRevision> added,
            VisiblePackages visiblePackages, Set<DependencyEntry> entries,
            Function<BundleRevision, ArtifactDescriptor> descriptorLookup) {
        if (module == null || !added.add(module)) {
            return;
        }

        addPlugin(module, true, visiblePackages, entries, descriptorLookup);

        for (BundleRevision fragment : getFragments(module)) {
            addDependencyViaImportPackage(fragment, added, visiblePackages, entries, descriptorLookup);
        }
    }

    private Collection<BundleRevision> getFragments(BundleRevision host) {
        if (host == null /* || !isExtensibleAPI(host) */) {
            return Collections.emptyList();
        }
        return host.getWiring().getProvidedWires(HostNamespace.HOST_NAMESPACE).stream().map(BundleWire::getRequirer)
                .toList();
    }

    private void addDependency(BundleRevision desc, Collection<BundleRevision> added, VisiblePackages visiblePackages,
            Set<DependencyEntry> entries, Function<BundleRevision, ArtifactDescriptor> descriptorLookup) {
        addDependency(desc, added, visiblePackages, entries, true, descriptorLookup);
    }

    private void addDependency(BundleRevision desc, Collection<BundleRevision> added, VisiblePackages visiblePackages,
            Set<DependencyEntry> entries, boolean useInclusion,
            Function<BundleRevision, ArtifactDescriptor> descriptorLookup) {
        if (desc == null || !added.add(desc))
            return;

        addPlugin(desc, useInclusion, visiblePackages, entries, descriptorLookup);

        // add fragments that are not patches after the host
        for (BundleRevision fragment : getFragments(desc)) {
            addDependency(fragment, added, visiblePackages, entries, useInclusion, descriptorLookup);
        }

        for (BundleRevision required : getRequiredBundles(desc)) {
            addDependency(required, added, visiblePackages, entries, useInclusion, descriptorLookup);
        }
    }

    private void addPlugin(BundleRevision module, boolean useInclusions, VisiblePackages visiblePackages,
            Set<DependencyEntry> entries, Function<BundleRevision, ArtifactDescriptor> descriptorLookup) {
        Collection<AccessRule> rules = useInclusions ? visiblePackages.getInclusions(module) : null;
        DependencyEntry entry = new DependencyEntry(module, descriptorLookup.apply(module), rules);
        entries.add(entry);
    }

    private void addHostPlugin(BundleRevision host, Set<BundleRevision> added, VisiblePackages visiblePackages,
            Set<DependencyEntry> entries, Function<BundleRevision, ArtifactDescriptor> descriptorLookup) {
        if (host == null) {
            return;
        }
        // add host plug-in
        if (added.add(host)) {
            addPlugin(host, false, visiblePackages, entries, descriptorLookup);
            for (BundleRevision required : getRequiredBundles(host)) {
                addDependency(required, added, visiblePackages, entries, descriptorLookup);
            }

            // add Import-Package
            host.getWiring().getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE).stream().map(BundleWire::getProvider)
                    .forEach(provider -> addDependencyViaImportPackage(provider, added, visiblePackages, entries,
                            descriptorLookup));
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

    @Override
    public List<AccessRule> computeBootClasspathExtraAccessRules(ModuleContainer container) {
        ModuleRevision systemBundle = container.getModule(Constants.SYSTEM_BUNDLE_ID).getCurrentRevision();
        return systemBundle.getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE).stream()
                .map(BundleWire::getRequirer).flatMap(systemFragment -> systemFragment
                        .getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE).stream())
                .map(packageExport -> createRule(systemBundle, packageExport)).toList();
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
