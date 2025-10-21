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

    public static class DependencyEntry {
        public final BundleRevision module;
        public final Collection<AccessRule> rules;
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
            if (!(obj instanceof DependencyEntry other))
                return false;
            return Objects.equals(module, other.module) && Objects.equals(rules, other.rules);
        }

        public BundleRevision getRevision() {
            return module;
        }

        public File getLocation() {
            Bundle bundle = module.getBundle();
            if (bundle == null) {
                //this must be a placeholder one...
                return null;
            }
            int bundleId = bundle.getBundleId();
            if (bundleId == Constants.SYSTEM_BUNDLE_ID) {
                return null;
            }
            return descriptor.fetchArtifact().orElseThrow();
        }

        public boolean isSystemBundle() {
            Bundle bundle = module.getBundle();
            if (bundle == null) {
                return false;
            }
            int bundleId = bundle.getBundleId();
            return bundleId == Constants.SYSTEM_BUNDLE_ID;
        }

        public String getSymbolicName() {
            return module.getSymbolicName();
        }

        public Version getVersion() {
            return module.getVersion();
        }

        @Override
        public String toString() {
            return getSymbolicName() + "_" + getVersion();
        }

        public ArtifactDescriptor getArtifactDescriptor() {
            return descriptor;
        }
    }

    private final Map<ModuleRevision, VisiblePackages> packagesCache = new HashMap<>();

    private final ModuleContainer container;

    public DefaultDependencyComputer(ModuleContainer container) {
        this.container = container;
    }

    public static class VisiblePackages {
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
     * @see #DefaultDependencyComputer(ModuleContainer)
     */
    @Override
    public List<DependencyEntry> computeDependencies(ModuleRevision module,
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
            if (added.contains(bundle)) {
                continue;
            }
            String bundleId = bundle.getSymbolicName() + "_" + bundle.getVersion();
            resolvedImportPackages.put(bundleId, bundle);
        }

        for (BundleRevision bundle : resolvedImportPackages.values()) {
            if (added.add(bundle)) {
                Collection<AccessRule> accessRules = visiblePackages.getInclusions(bundle);
                DependencyEntry entry = new DependencyEntry(bundle, descriptorLookup.apply(bundle), accessRules);
                entries.add(entry);
            }
        }

        return new ArrayList<>(entries);
    }

    private static Optional<BundleRevision> getFragmentHost(BundleRevision fragment) {
        List<BundleWire> wires = fragment.getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
        return wires.isEmpty() ? Optional.empty() : Optional.of(wires.getFirst().getProvider());
    }

    private static Collection<BundleRevision> getRequiredBundles(BundleRevision bundle) {
        List<BundleWire> requiredBundles = bundle.getWiring().getRequiredModuleWires(BundleNamespace.BUNDLE_NAMESPACE);
        return requiredBundles.stream().map(BundleWire::getProvider).toList();
    }

    private static void addHostPlugin(BundleRevision host, Set<BundleRevision> added, VisiblePackages visiblePackages,
            Set<DependencyEntry> entries, Function<BundleRevision, ArtifactDescriptor> descriptorLookup) {
        if (added.add(host)) {
            Collection<AccessRule> rules = visiblePackages.getInclusions(host);
            DependencyEntry entry = new DependencyEntry(host, descriptorLookup.apply(host), rules);
            entries.add(entry);
        }
    }

    private static void addDependency(BundleRevision bundle, Set<BundleRevision> added,
            VisiblePackages visiblePackages, Set<DependencyEntry> entries,
            Function<BundleRevision, ArtifactDescriptor> descriptorLookup) {
        if (added.add(bundle)) {
            Collection<AccessRule> rules = visiblePackages.getInclusions(bundle);
            DependencyEntry entry = new DependencyEntry(bundle, descriptorLookup.apply(bundle), rules);
            entries.add(entry);
        }
    }

    private VisiblePackages getPackagesInternal(ModuleRevision module) {
        return packagesCache.computeIfAbsent(module, m -> {
            VisiblePackages packages = new VisiblePackages(m);
            for (ModuleWire wire : getPackageImportWires(m)) {
                ModuleWire packageWire = wire;
                do {
                    packages.add(packageWire.getCapability());
                    // Need to check if the capability must be added to the packages because of a reexport
                    // In case the current provider reexports a package that it also imports from another
                    // bundle we need to make sure that this bundle is added to the visible packages too
                    Optional<ModuleWire> reexportedPackageWire = getReexportedPackageWire(packageWire);
                    if (reexportedPackageWire.isPresent()) {
                        packageWire = reexportedPackageWire.get();
                    } else {
                        packageWire = null;
                    }
                } while (packageWire != null);
            }
            for (BundleRevision requiredRevision : getRequiredBundles(m)) {
                addPublicPackages(requiredRevision, packages);
            }
            return packages;
        });
    }

    private static List<ModuleWire> getPackageImportWires(ModuleRevision module) {
        List<ModuleWire> wires = module.getWiring().getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
        // A fragment's package imports are resolved against the host's wiring
        getFragmentHost(module).ifPresent(
                host -> wires.addAll(host.getWiring().getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE)));
        return wires;
    }

    private static void addPublicPackages(BundleRevision bundle, VisiblePackages packages) {
        ModuleWiring wiring = bundle.getWiring();
        List<ModuleCapability> declaredCapabilities = bundle
                .getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
        for (ModuleCapability packageCapability : declaredCapabilities) {
            // A public package is a package which is exported and not internal
            if (isPublicPackage(bundle, packageCapability)) {
                packages.add(packageCapability);
            }
        }

        // Bundle re-exports?
        for (BundleWire requiredBundle : wiring.getRequiredModuleWires(BundleNamespace.BUNDLE_NAMESPACE)) {
            if (hasVisibilityReexport(requiredBundle)) {
                addPublicPackages(requiredBundle.getProvider(), packages);
            }
        }
    }

    private static Optional<ModuleWire> getReexportedPackageWire(ModuleWire packageWire) {
        // The provider of the package capability could import the package it exports (e.g. for split packages)
        ModuleWiring providerWiring = packageWire.getProvider().getWiring();
        ModuleCapability packageCapability = packageWire.getCapability();
        List<ModuleWire> packageImportWires = providerWiring
                .getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
        for (ModuleWire importWire : packageImportWires) {
            if (Objects.equals(importWire.getCapability().getAttributes(), packageCapability.getAttributes())) {
                BundleRevision reexportingBundle = importWire.getProvider();
                // The module reexports the package if it provides (=exports) the package capability again
                for (ModuleCapability candidate : reexportingBundle.getCapabilities(null)) {
                    if (Objects.equals(candidate.getAttributes(), packageCapability.getAttributes())) {
                        return Optional.of(importWire);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isPublicPackage(BundleRevision bundle, Capability packageExport) {
        return !isDiscouragedAccess(bundle, packageExport);
    }

    private static AccessRule createRule(BundleRevision bundle, Capability packageExport) {
        String pkgName = (String) packageExport.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
        boolean discouraged = isDiscouragedAccess(bundle, packageExport);
        return new DefaultAccessRule(pkgName, discouraged);
    }

    /**
     * Computes and returns extra access rules for boot classpath, i.e. packages exported by system
     * bundle [1]
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
