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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.internal.resolver.StateImpl;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;

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

    public List<DependencyEntry> computeDependencies(ModuleRevision module) {
        if (module == null) {
            return Collections.emptyList();
        }

        ArrayList<DependencyEntry> entries = new ArrayList<>();
        Multimap<ModuleRevision, AccessRule> visiblePackages = retrieveVisiblePackagesFromState(module);
        Set<ModuleRevision> added = new HashSet<>();

        // to avoid cycles, e.g. when a bundle imports a package it exports
        added.add(module);

        getFragmentHost(module).ifPresent(host -> addHostPlugin(host, added, visiblePackages, entries));
        getRequiredBundles(module).forEach(required -> addDependency(required, added, visiblePackages, entries));

        // add Import-Package
        // sort by symbolicName_version to get a consistent order
        Map<String, ModuleRevision> resolvedImportPackages = new TreeMap<>();
        for (ModuleRevision bundle : visiblePackages.keySet()) {
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
        return module.getWiring().getRequiredModuleWires(BundleRevision.BUNDLE_NAMESPACE).stream()
                .map(ModuleWire::getProvider).collect(Collectors.toList());
    }

    private Optional<ModuleRevision> getFragmentHost(ModuleRevision bundleRevision) {
        return bundleRevision.getWiring().getRequiredModuleWires(BundleRevision.HOST_NAMESPACE).stream()
                .map(ModuleWire::getProvider).findAny();
    }

    private Multimap<ModuleRevision, AccessRule> retrieveVisiblePackagesFromState(ModuleRevision module) {
        Multimap<ModuleRevision, AccessRule> visiblePackages = LinkedHashMultimap.create();
        if (module != null) {
            addVisiblePackagesFromState(module, visiblePackages);
            getFragmentHost(module)
                    .ifPresent(fragmentHost -> addVisiblePackagesFromState(fragmentHost, visiblePackages));
        }
        return visiblePackages;
    }

    private void addVisiblePackagesFromState(ModuleRevision module,
            Multimap<ModuleRevision, AccessRule> visiblePackages) {
        if (module == null) {
            return;
        }
        Stream.concat(
                module.getWiring().getRequiredModuleWires(BundleRevision.PACKAGE_NAMESPACE).stream()
                        .map(ModuleWire::getCapability), //
                module.getWiring().getRequiredModuleWires(BundleRevision.BUNDLE_NAMESPACE).stream()
                        .map(ModuleWire::getProvider).flatMap(requiredBundle -> requiredBundle
                                .getModuleCapabilities(BundleRevision.PACKAGE_NAMESPACE).stream()) //
        ).forEach(exportCapability -> visiblePackages.put(exportCapability.getResource(),
                getRule(module, exportCapability)));
    }

    private AccessRule getRule(ModuleRevision consumer, Capability export) {
        String name = (String) export.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
        String path = (name.equals(".")) ? "*" : name.replace('.', '/') + "/*";
        return new DefaultAccessRule(path, isDiscouragedAccess(consumer, export));
    }

    protected void addDependencyViaImportPackage(ModuleRevision module, Collection<ModuleRevision> added,
            Multimap<ModuleRevision, AccessRule> map, Collection<DependencyEntry> entries) {
        if (module == null || !added.add(module)) {
            return;
        }

        addPlugin(module, true, map, entries);

        for (ModuleRevision fragment : getFragments(module)) {
            addDependencyViaImportPackage(fragment, added, map, entries);
        }
    }

    private Collection<ModuleRevision> getFragments(ModuleRevision host) {
        if (host == null /* || !isExtensibleAPI(host) */) {
            return Collections.emptyList();
        }

        return host.getWiring().getProvidedModuleWires(BundleRevision.HOST_NAMESPACE).stream()
                .map(ModuleWire::getRequirer).collect(Collectors.toList());
    }

    private void addDependency(ModuleRevision desc, Collection<ModuleRevision> added,
            Multimap<ModuleRevision, AccessRule> map, Collection<DependencyEntry> entries) {
        addDependency(desc, added, map, entries, true);
    }

    private void addDependency(ModuleRevision desc, Collection<ModuleRevision> added,
            Multimap<ModuleRevision, AccessRule> map, Collection<DependencyEntry> entries, boolean useInclusion) {
        if (desc == null || !added.add(desc))
            return;

        addPlugin(desc, useInclusion, map, entries);

        // add fragments that are not patches after the host
        for (ModuleRevision fragment : getFragments(desc)) {
            addDependency(fragment, added, map, entries, useInclusion);
        }

        for (ModuleRevision required : getRequiredBundles(desc)) {
            addDependency(required, added, map, entries, useInclusion);
        }
    }

    private void addPlugin(ModuleRevision module, boolean useInclusions, Multimap<ModuleRevision, AccessRule> map,
            Collection<DependencyEntry> entries) {
        Collection<AccessRule> rules = useInclusions ? getInclusions(map, module) : null;
        DependencyEntry entry = new DependencyEntry(module, rules);
        if (!entries.contains(entry)) {
            entries.add(entry);
        }
    }

    private Collection<AccessRule> getInclusions(Multimap<ModuleRevision, AccessRule> map, ModuleRevision module) {
        Collection<AccessRule> rules = map.get(getFragmentHost(module).orElse(module));
        return rules != null ? rules : Collections.emptyList();
    }

    private void addHostPlugin(ModuleRevision host, Collection<ModuleRevision> added,
            Multimap<ModuleRevision, AccessRule> map, Collection<DependencyEntry> entries) {
        if (host == null) {
            return;
        }
        // add host plug-in
        if (added.add(host)) {
            addPlugin(host, false, map, entries);
            for (ModuleRevision required : getRequiredBundles(host)) {
                addDependency(required, added, map, entries);
            }

            // add Import-Package
            host.getWiring().getRequiredModuleWires(BundleRevision.PACKAGE_NAMESPACE).stream()
                    .map(ModuleWire::getProvider)
                    .forEach(provider -> addDependencyViaImportPackage(provider, added, map, entries));
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
        ModuleRevision systemBundle = container.getModule(Constants.SYSTEM_BUNDLE_ID).getCurrentRevision();
        return systemBundle.getWiring().getProvidedModuleWires(BundleRevision.HOST_NAMESPACE).stream()
                .map(ModuleWire::getRequirer)
                .flatMap(systemFragment -> systemFragment.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE)
                        .stream())
                .map(packageExport -> getRule(systemBundle, packageExport)).collect(Collectors.toList());
    }

    public static boolean isDiscouragedAccess(BundleRevision bundle, Capability export) {
        if (Boolean.parseBoolean(export.getDirectives().get(StateImpl.INTERNAL_DIRECTIVE))) {
            return true;
        }
        String allFriends = export.getDirectives().get(StateImpl.FRIENDS_DIRECTIVE);
        if (allFriends != null) {
            return !Arrays.asList(allFriends.split(",")).contains(bundle.getSymbolicName());
        }
        return false;
    }

}
