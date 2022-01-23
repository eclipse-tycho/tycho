/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.build;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.DefaultGraphBuilder;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.model.building.Result;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = GraphBuilder.class, hint = GraphBuilder.HINT)
public class TychoGraphBuilder extends DefaultGraphBuilder {

    @Override
    public Result<ProjectDependencyGraph> build(MavenSession session) {
        System.out.println("TychoGraphBuilder.build()");
        MavenExecutionRequest request = session.getRequest();
        ProjectDependencyGraph dependencyGraph = session.getProjectDependencyGraph();
        System.out.println("  - SelectedProjects: " + request.getSelectedProjects());
        System.out.println("  - ExcludedProjects: " + request.getExcludedProjects());
        System.out.println("  - MakeBehavior:     " + request.getMakeBehavior());
        System.out.println("  - DependencyGraph:  " + dependencyGraph);

        return super.build(session);
    }

}
