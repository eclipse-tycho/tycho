/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.facade.TargetEnvironment;

public class EnvironmentSpecificConfigurations {

    static class Entry {
        TargetEnvironmentFilter envFilter;
        EnvironmentSpecificConfiguration config;

        Entry(String env, EnvironmentSpecificConfiguration config) {
            this.envFilter = TargetEnvironmentFilter.parseConfigurationElement(env);
            this.config = config;
        }
    }

    private final EnvironmentSpecificConfiguration globalConfiguration;
    private List<Entry> specificConfigurations;

    EnvironmentSpecificConfigurations(EnvironmentSpecificConfiguration globalConfig, List<Entry> specificConfigurations) {
        this.globalConfiguration = globalConfig;
        this.specificConfigurations = specificConfigurations;
    }

    public static EnvironmentSpecificConfigurations globalOnly(EnvironmentSpecificConfiguration globalConfiguration) {
        return new EnvironmentSpecificConfigurations(globalConfiguration, Collections.<Entry> emptyList());
    }

    public static EnvironmentSpecificConfigurations parse(EnvironmentSpecificConfiguration globalConfiguration,
            Set<String> environmentsWithConfiguration, Xpp3Dom rawConfiguration) throws MojoExecutionException {
        return new EnvironmentSpecificConfigurations(globalConfiguration, parseSpecificConfigurations(
                environmentsWithConfiguration, rawConfiguration));
    }

    private static List<Entry> parseSpecificConfigurations(Set<String> environmentsWithConfiguration, Xpp3Dom mapDom)
            throws MojoExecutionException {
        List<Entry> result = new ArrayList<Entry>();
        for (String environment : environmentsWithConfiguration) {
            // get the configuration value (that Plexus wasn't able to parse for us)
            Xpp3Dom entryDom = mapDom.getChild(environment);
            if (entryDom == null) {
                throw new MojoExecutionException("Could not find raw configuration for envSpecificConfiguration/"
                        + environment);
            }
            result.add(new Entry(environment, EnvironmentSpecificConfiguration.parse(entryDom)));
        }
        return result;
    }

    /**
     * Returns the value of the given parameter for the configuration with the most specific,
     * matching environment filter. Configurations that do not define a value for the parameter are
     * ignored, i.e. <code>null</code> values of more specific configurations don't override an
     * explicit value specified for a less specific environment filter.
     * 
     * @param parameter
     *            The parameter whose effective value to obtain.
     * @param environment
     *            The target environment for which to obtain the effective value.
     */
    public String getEffectiveValue(EnvironmentSpecificConfiguration.Parameter parameter, TargetEnvironment environment) {
        EnvironmentSpecificConfiguration matchingConfiguration = globalConfiguration;
        int matchingFields = 0;
        for (Entry entry : specificConfigurations) {
            if (entry.config.getValue(parameter) != null && entry.envFilter.matches(environment)
                    && entry.envFilter.filteredFieldCount() >= matchingFields) {
                matchingConfiguration = entry.config;
                matchingFields = entry.envFilter.filteredFieldCount();
            }
        }
        return matchingConfiguration.getValue(parameter);
    }
}
