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
 *    SAP AG - move optionalDependencies parameter to the compiler plugin (bug 351842)
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;

@Component(role = CompilerOptionsManager.class)
public class CompilerOptionsManager {

    private static final String OPTIONAL_RESOLUTION_REQUIRE = "require";
    private static final String OPTIONAL_RESOLUTION_IGNORE = "ignore";

    public CompilerOptions getCompilerOptions(MavenProject project) {

        Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-compiler-plugin");

        OptionalResolutionAction optionalAction = null;
        List<Dependency> extraRequirements = null;
        if (plugin != null) {
            // TODO check version?
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
            if (configuration != null) {
                optionalAction = getOptionalResolutionAction(configuration);
                extraRequirements = getExtraRequirements(configuration);
            }
        }

        if (optionalAction == null) {
            optionalAction = OptionalResolutionAction.REQUIRE;
        }
        return new CompilerOptions(optionalAction, extraRequirements);
        // TODO cache this value by attaching it to the project?
    }

    private OptionalResolutionAction getOptionalResolutionAction(Xpp3Dom configuration) {
        Xpp3Dom optionalDependenciesDom = configuration.getChild("optionalDependencies");
        if (optionalDependenciesDom == null) {
            return null;
        }

        String optionalDependencies = optionalDependenciesDom.getValue();

        if (OPTIONAL_RESOLUTION_REQUIRE.equals(optionalDependencies)) {
            return OptionalResolutionAction.REQUIRE;
        } else if (OPTIONAL_RESOLUTION_IGNORE.equals(optionalDependencies)) {
            return OptionalResolutionAction.IGNORE;
        } else {
            throw new RuntimeException("Illegal value of <optionalDependencies> compiler parameter: "
                    + optionalDependencies);
        }
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
