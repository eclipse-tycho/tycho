/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Rapicorp, Inc. - add support for IU type (428310)
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.internal.p2.updatesite.SiteBundle;
import org.eclipse.equinox.internal.p2.updatesite.SiteFeature;
import org.eclipse.equinox.internal.p2.updatesite.SiteIU;
import org.eclipse.equinox.internal.p2.updatesite.SiteModel;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

@SuppressWarnings("restriction")
public abstract class AbstractSiteDependenciesAction extends AbstractDependenciesAction {

    private final String id;

    private final String version;

    public AbstractSiteDependenciesAction(String id, String version) {
        this.id = id;
        this.version = version;
    }

    abstract SiteModel getSiteModel();

    @Override
    protected Set<IRequirement> getRequiredCapabilities() {
        Set<IRequirement> required = new LinkedHashSet<IRequirement>();

        for (SiteFeature feature : getSiteModel().getFeatures()) {
            String id = feature.getFeatureIdentifier() + FEATURE_GROUP_IU_SUFFIX; //$NON-NLS-1$

            VersionRange range = getVersionRange(createVersion(feature.getFeatureVersion()));

            required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, range, null, false,
                    false));
        }

        for (SiteBundle bundle : getSiteModel().getBundles()) {
            String id = bundle.getBundleIdentifier();
            VersionRange range = getVersionRange(createVersion(bundle.getBundleVersion()));
            required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, range, null, false,
                    false));
        }

        for (SiteIU iu : getSiteModel().getIUs()) {
            IRequirement requirement = getRequirement(iu);
            if (requirement != null)
                required.add(requirement);
        }

        return required;
    }

    private IRequirement getRequirement(SiteIU iu) {
        String id = iu.getID();
        String range = iu.getRange();
        String type = iu.getQueryType();
        String expression = iu.getQueryExpression();
        String[] params = iu.getQueryParams();
        if (id != null) {
            VersionRange vRange = new VersionRange(range);
            return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, vRange, null, false, false);
        } else if (type.equals("match")) {
            //Merge the arguments
            String[] allArgs = new String[(params == null ? 0 : params.length) + 1];
            allArgs[0] = id;
            if (params != null)
                System.arraycopy(params, 0, allArgs, 1, params.length);

            IMatchExpression<IInstallableUnit> iuMatcher = ExpressionUtil.getFactory()
                    .<IInstallableUnit> matchExpression(ExpressionUtil.parse(expression), id); //$NON-NLS-1$
            return MetadataFactory.createRequirement(iuMatcher, null, 0, 1, true);
        } else if (type.equals("context")) {
            throw new IllegalStateException("Context iu queries are not supported in Tycho. Faulty expression is "
                    + expression);
        }
        return null;
    }

    @Override
    protected String getId() {
        return id;
    }

    @Override
    protected Version getVersion() {
        return createSiteVersion(version);
    }

    public static Version createSiteVersion(String version) {
        try {
            // try default (OSGi?) format first
            return Version.create(version);
        } catch (IllegalArgumentException e) {
            // treat as raw otherwise
            return Version.create("format(n[.n=0;[.n=0;['-'S]]]):" + version);
        }
    }
}
