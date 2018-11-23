/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;

@Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_TEST_PLUGIN)
public class OsgiTestBundleProject extends OsgiBundleProject {

    @Override
    public void readExecutionEnvironmentConfiguration(MavenProject project, ExecutionEnvironmentConfiguration sink) {
        super.readExecutionEnvironmentConfiguration(project, sink);
        // for test plugins, use the execution environment of the currently running JDK
        // TODO we should probably have a switch to go back to the old behaviour
        // TODO currently running profile should not be used in case useJDK=BREE in TestMojo
        sink.setProfileConfiguration(calculateCurrentProfileName(), "currently running JDK");
    }

    private String calculateCurrentProfileName() {
        // HACK hardcoded. we need to find a way get the EE profile name of the currently running JDK
        return "JavaSE-11";
    }

}
