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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.model;

import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.VersionRange;

class FeatureResourceBuilder extends ResourceBuilder {

    static final String P2_FEATURE = "p2.feature";

    static final String FEATURE_JAR_SUFFIX = ".feature.jar";

    static final String P2_GROUP = "p2.group";

    public void addRequireFeature(String id, String versionRange) {
        RequirementBuilder feature = new RequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
        Attrs featureAttrs = new Attrs();
        featureAttrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, P2_GROUP);
        feature.addFilter(IdentityNamespace.IDENTITY_NAMESPACE, id, versionRange, featureAttrs);
        addRequirement(feature);
    }

    public void addFeatureGroupCapability(String id, String version) {
        CapabilityBuilder identity = new CapabilityBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
        identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, id + ".feature.group");
        identity.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
        identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, P2_GROUP);
        addCapability(identity);
    }

    public void addFeatureJarRequirement(String id, String version) {
        RequirementBuilder featureJar = new RequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
        Attrs featureJarAttrs = new Attrs();
        featureJarAttrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, P2_FEATURE);
        featureJar.addFilter(IdentityNamespace.IDENTITY_NAMESPACE, id + FEATURE_JAR_SUFFIX,
                "[" + version + "," + version + "]", featureJarAttrs);
        addRequirement(featureJar);
    }

    public void addFeatureJarCapability(String id, String version) {
        CapabilityBuilder identity = new CapabilityBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
        identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, id + FEATURE_JAR_SUFFIX);
        identity.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
        identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, P2_FEATURE);
        addCapability(identity);
    }

    public void addRuntimeBundleRequirement(String bsn, VersionRange range) {
        Attrs attrs = new Attrs();
        attrs.put(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, range.toString());
        RequirementBuilder require = new RequirementBuilder(BundleNamespace.BUNDLE_NAMESPACE);
        require.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE);
        require.addFilter(BundleNamespace.BUNDLE_NAMESPACE, bsn,
                attrs.get(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE), attrs);
        addRequirement(require);
    }

    public void addRuntimeFeature(String id, VersionRange range) {
        RequirementBuilder feature = new RequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
        Attrs featureAttrs = new Attrs();
        featureAttrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, P2_GROUP);
        feature.addFilter(IdentityNamespace.IDENTITY_NAMESPACE, id, range.toString(), featureAttrs);
        feature.addDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, Namespace.EFFECTIVE_ACTIVE);
        addRequirement(feature);

    }

}
