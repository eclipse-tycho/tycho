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

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

class RawConfigurationParserHelper {

    static Xpp3Dom getRawMojoConfiguration(MavenProject project, String pluginId, String parameter)
            throws MojoExecutionException {
        Plugin plugin = project.getPlugin("org.eclipse.tycho:" + pluginId);
        if (plugin == null) {
            throw new MojoExecutionException("Could not find raw plugin configuration of " + pluginId);
        }

        // TODO fail if there is execution-specific configuration -> we don't know which execution is active 
//        plugin.getExecutions() ...

        Xpp3Dom configurationDom = (Xpp3Dom) plugin.getConfiguration();
        if (configurationDom == null) {
            throw new MojoExecutionException("Could not find raw plugin configuration of " + pluginId);
        }
        Xpp3Dom parameterDom = configurationDom.getChild(parameter);
        if (parameterDom == null)
            throw new MojoExecutionException("Could not find raw configuration parameter " + parameter);
        return parameterDom;
    }

    public static String getStringValue(Xpp3Dom element) {
        if (element == null) {
            return null;
        }
        String value = element.getValue().trim();
        if ("".equals(value)) {
            return null;
        }
        return value;
    }

    public static String getStringValueOfChild(Xpp3Dom dom, String childName) {
        return getStringValue(dom.getChild(childName));
    }

}
