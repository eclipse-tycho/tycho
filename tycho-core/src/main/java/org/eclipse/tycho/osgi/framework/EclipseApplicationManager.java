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
package org.eclipse.tycho.osgi.framework;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.Repository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;

@Component(role = EclipseApplicationManager.class)
public class EclipseApplicationManager {

    private static final String FRAGMENT_COMPATIBILITY = "org.eclipse.osgi.compatibility.state";

    private static final String FILTER_MACOS = "(osgi.os=macosx)";

    public static MavenRepositoryLocation getRepository(Repository location) {
        if (location == null) {
            return new MavenRepositoryLocation(null, URI.create(TychoConstants.ECLIPSE_LATEST));
        }
        return new MavenRepositoryLocation(location.getId(), URI.create(location.getUrl()));
    }

    private final Map<URI, TargetPlatform> targetPlatformCache = new ConcurrentHashMap<>();

    private final Map<TargetCacheKey, EclipseApplication> applicationCache = new ConcurrentHashMap<>();

    @Requirement
    private EclipseApplicationFactory applicationFactory;

    public EclipseApplication getApplication(TargetPlatform targetPlatform, Bundles bundles, Features features,
            String name) {

        return applicationCache.computeIfAbsent(new TargetCacheKey(targetPlatform, bundles, features), key -> {
            EclipseApplication application = applicationFactory.createEclipseApplication(key.targetPlatform(), name);
            addBundlesAndFeatures(bundles, features, application);
            return application;
        });
    }

    public EclipseApplication getApplication(Repository location, Bundles bundles, Features features, String name) {
        return getApplication(getRepository(location), bundles, features, name);
    }

    public EclipseApplication getApplication(MavenRepositoryLocation repository, Bundles bundles, Features features,
            String name) {
        TargetPlatform targetPlatform = targetPlatformCache.computeIfAbsent(repository.getURL().normalize(),
                x -> applicationFactory.createTargetPlatform(List.of(repository)));
        return getApplication(targetPlatform, bundles, features, name);
    }

    private void addBundlesAndFeatures(Bundles bundles, Features features, EclipseApplication application) {
        for (String bsn : bundles.bundles()) {
            //TODO can we do this after resolve and check to add additional things?!?
            if (Bundles.BUNDLE_PDE_CORE.equals(bsn) || Bundles.BUNDLE_API_TOOLS.equals(bsn)) {
                //PDE requires compatibility
                application.addBundle(FRAGMENT_COMPATIBILITY);
            }
            if (Bundles.BUNDLE_API_TOOLS.equals(bsn)) {
                application.addConditionalBundle(Bundles.BUNDLE_LAUNCHING_MACOS, FILTER_MACOS);
            }
            application.addBundle(bsn);
        }
        for (String feature : features.features()) {
            application.addFeature(feature);
        }
    }

    private static record TargetCacheKey(TargetPlatform targetPlatform, Bundles bundles, Features feature) {

    }
}
