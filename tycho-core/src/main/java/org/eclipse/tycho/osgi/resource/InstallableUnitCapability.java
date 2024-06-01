/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

/**
 * Maps an {@link IProvidedCapability} of an {@link IInstallableUnit} to a
 * <a href="https://docs.osgi.org/specification/osgi.core/8.0.0/framework.resource.html">Resource
 * API Specification</a> {@link Capability}
 */
public class InstallableUnitCapability implements Capability {

    private IProvidedCapability capability;
    private InstallableUnitResource resource;
    private Map<String, Object> attributes;
    private final String namespace;

    public InstallableUnitCapability(InstallableUnitResource resource, IProvidedCapability capability) {
        this.resource = resource;
        this.capability = capability;
        Map<String, Object> properties = capability.getProperties();
        Map<String, Object> map = new HashMap<>(properties);
        String ns = capability.getNamespace();
        if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(ns)) {
//      <capability namespace='osgi.wiring.package'>
//        <attribute name='osgi.wiring.package' value='org.acme.pool'/>
//        <attribute name='version' type='Version' value='1.1.2'/>
//        <attribute name='bundle-version' type='Version' value='1.5.6'/>
//        <attribute name='bundle-symbolic-name' value='org.acme.pool'/>
//        <directive name='uses' value='org.acme.pool,org.acme.util'/>
//      </capability>
            namespace = PackageNamespace.PACKAGE_NAMESPACE;
            Object packageName = properties.get(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE);
            if (packageName instanceof String) {
                map.put(PackageNamespace.PACKAGE_NAMESPACE, packageName);
            }
            map.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(capability.getVersion().toString()));
            findBundle(resource.installableUnit).ifPresent(bundle -> {
                Object bundleName = bundle.getProperties().get(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE);
                if (bundleName instanceof String) {
                    map.put(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, bundleName);
                }
                map.put(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
                        new Version(bundle.getVersion().toString()));
            });
        } else if (BundlesAction.CAPABILITY_NS_OSGI_BUNDLE.equals(ns)) {
//          <capability namespace='osgi.wiring.bundle'>
//            <attribute name='osgi.wiring.bundle' value='org.acme.pool'/>
//            <attribute name='bundle-version' type='Version' value='1.5.6'/>
//          </capability>
            namespace = BundleNamespace.BUNDLE_NAMESPACE;
            Object bundleName = properties.get(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE);
            if (bundleName instanceof String) {
                map.put(BundleNamespace.BUNDLE_NAMESPACE, bundleName);
            }
            map.put(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
                    new Version(capability.getVersion().toString()));
        } else {
            //generic namespace definition e.g.
//          <capability namespace='osgi.identity'>
//            <attribute name='osgi.identity' value='org.acme.pool'/>
//            <attribute name='version'type='Version' value='1.5.6'/>
//            <attribute name='type' value='osgi.bundle'/>
//          </capability>
            map.put("version", new Version(capability.getVersion().toString()));
            namespace = capability.getNamespace();
        }
        this.attributes = Map.copyOf(map);
    }

    private Optional<IProvidedCapability> findBundle(IInstallableUnit installableUnit) {
        return installableUnit.getProvidedCapabilities().stream().filter(IProvidedCapability.class::isInstance)
                .map(IProvidedCapability.class::cast)
                .filter(cap -> BundlesAction.CAPABILITY_NS_OSGI_BUNDLE.equals(cap.getNamespace())).findFirst();
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public Map<String, String> getDirectives() {
        return Map.of();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return capability.toString();
    }

}
