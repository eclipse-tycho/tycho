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

import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Maps a {@link IRequirement} of an {@link IInstallableUnit} into a
 * <a href="https://docs.osgi.org/specification/osgi.core/8.0.0/framework.resource.html">Resource
 * API Specification</a> {@link Requirement}
 * 
 */
public class InstallableUnitRequirement implements Requirement {

    private static final String VERSION_REQUIREMENT_FILTER = "(&(%s=%s)(%s%s%s))";
    private static final String VERSION_RANGE_REQUIREMENT_FILTER = "(&(%s=%s)(%s%s%s)(!(%s%s%s)))";

    private final InstallableUnitResource resource;
    private final String namespace;
    private final Map<String, String> directives;

    private final IRequirement requirement;

    public InstallableUnitRequirement(InstallableUnitResource p2Resource, IRequirement requirement) {
        this.requirement = requirement;
        Map<String, String> map = new HashMap<>(2);
        IMatchExpression<IInstallableUnit> expression = requirement.getMatches();
        String name = RequiredCapability.extractName(expression);
        String ns = RequiredCapability.extractNamespace(expression);
        VersionRange range = RequiredCapability.extractRange(expression);
        String versionAttribute;
        if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(ns)) {
            this.namespace = PackageNamespace.PACKAGE_NAMESPACE;
            versionAttribute = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        } else if (BundlesAction.CAPABILITY_NS_OSGI_BUNDLE.equals(ns)) {
            this.namespace = BundleNamespace.BUNDLE_NAMESPACE;
            versionAttribute = BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
        } else {
            this.namespace = ns;
            versionAttribute = "version";
        }
        Version minimum = range.getMinimum();
        Version maximum = range.getMaximum();
        String filter;
        if (Version.MAX_VERSION.equals(maximum)) {
            filter = String.format(VERSION_REQUIREMENT_FILTER, namespace, name, //
                    versionAttribute, range.getIncludeMinimum() ? ">=" : ">", minimum.toString());
        } else {
            filter = String.format(VERSION_RANGE_REQUIREMENT_FILTER, namespace, name, //
                    versionAttribute, range.getIncludeMinimum() ? ">=" : ">", minimum.toString(), versionAttribute,
                    range.getIncludeMaximum() ? ">" : ">=", maximum.toString());
        }
        map.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
        if (requirement.getMin() == 0) {
            map.put(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);
        }
        this.directives = Map.copyOf(map);
        this.resource = p2Resource;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public Map<String, String> getDirectives() {
        return directives;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return requirement.toString();
    }

}
