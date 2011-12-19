/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - move extraRequirements parameter to the compiler plugin (bug 363331)
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;

@Component(role = CompilerOptionsManager.class)
public class CompilerOptionsManager {

    public CompilerOptions getCompilerOptions(MavenProject project) {

        Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-compiler-plugin");

        List<Dependency> extraRequirements = null;
        if (plugin != null) {
            // TODO check version?
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
            if (configuration != null) {
                extraRequirements = getExtraRequirements(configuration);
            }
        }

        return new CompilerOptions(extraRequirements);
    }

    private List<Dependency> getExtraRequirements(Xpp3Dom configuration) {
        List<Dependency> result = new ArrayList<Dependency>();
        Xpp3Dom requirementsDom = configuration.getChild("extraRequirements");
        if (requirementsDom != null) {
            for (Xpp3Dom requirementDom : requirementsDom.getChildren()) {
                result.add(newRequirement(requirementDom));
            }
        }
        return result;
    }

    private Dependency newRequirement(Xpp3Dom requirementDom) {
        Dependency d = new Dependency();
        d.setType(requirementDom.getChild("type").getValue());
        d.setArtifactId(requirementDom.getChild("id").getValue());
        d.setVersion(requirementDom.getChild("versionRange").getValue());
        return d;
    }
}
