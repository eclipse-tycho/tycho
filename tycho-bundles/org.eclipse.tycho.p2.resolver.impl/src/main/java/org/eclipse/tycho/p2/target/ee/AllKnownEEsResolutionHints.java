/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;
import org.osgi.framework.BundleActivator;

public class AllKnownEEsResolutionHints implements ExecutionEnvironmentResolutionHints {

    private final Map<VersionedId, IInstallableUnit> additionalUnits;
    private final Map<VersionedId, IInstallableUnit> temporaryUnits;

    public AllKnownEEsResolutionHints() {
        additionalUnits = new LinkedHashMap<VersionedId, IInstallableUnit>();
        for (String env : getAllKnownExecutionEnvironments()) {
            StandardEEResolutionHints.addIUsFromEnvironment(env, additionalUnits);
        }
        this.temporaryUnits = StandardEEResolutionHints.computeTemporaryAdditions(additionalUnits);
    }

    @Override
    public Collection<IInstallableUnit> getMandatoryUnits() {
        return additionalUnits.values();
    }

    @Override
    public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
        // See JREAction
        return iu.getId().startsWith("a.jre") || iu.getId().startsWith("config.a.jre");
    }

    @Override
    public boolean isEESpecificationUnit(IInstallableUnit unit) {
        // not needed
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<IRequirement> getMandatoryRequires() {
        // not needed; getMandatoryUnits already enforces the use of the JRE IUs during resolution
        return Collections.emptyList();
    }

    @Override
    public Collection<IInstallableUnit> getTemporaryAdditions() {
        return temporaryUnits.values();
    }

    private static List<String> getAllKnownExecutionEnvironments() {
        ClassLoader loader = BundleActivator.class.getClassLoader();
        Properties listProps = readProperties(loader.getResource("profile.list"));
        List<String> result = new ArrayList<String>();
        for (String profileFile : listProps.getProperty("java.profiles").split(",")) {
            Properties props = readProperties(loader.getResource(profileFile.trim()));
            String profileName = props.getProperty("osgi.java.profile.name");
            if (profileName == null) {
                throw new IllegalStateException("osgi.java.profile.name must not be null for profile " + profileFile);
            }
            result.add(profileName);
        }
        return result;
    }

    private static Properties readProperties(final URL url) {
        Properties listProps = new Properties();
        InputStream stream = null;
        try {
            stream = url.openStream();
            listProps.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return listProps;
    }

}
