/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildorder.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.buildorder.BuildOrderParticipant;
import org.eclipse.tycho.buildorder.model.BuildOrder.Export;
import org.eclipse.tycho.buildorder.model.BuildOrder.Import;
import org.eclipse.tycho.buildorder.model.BuildOrderRelations;

public class BuildOrderManager {

    private final List<BuildOrderParticipant> buildOrderParticipants;
    private final MavenSession session;

    private Map<MavenProject, List<Export>> allExports;
    private Map<MavenProject, List<Import>> allImports;

    public BuildOrderManager(List<BuildOrderParticipant> buildOrderParticipants, MavenSession session) {
        this.buildOrderParticipants = buildOrderParticipants;
        this.session = session;
    }

    public void orderProjects() {
        allExports = new HashMap<MavenProject, List<Export>>();
        allImports = new HashMap<MavenProject, List<Import>>();

        collectExportsAndImports();
        injectDependenciesForMatches();
    }

    private void collectExportsAndImports() {
        List<MavenProject> projects = session.getProjects();
        for (BuildOrderParticipant buildOrderParticipant : buildOrderParticipants) {
            for (MavenProject project : projects) {
                BuildOrderRelations relations = buildOrderParticipant.getRelationsOf(project);

                addExports(project, relations.getExports());
                addImports(project, relations.getImports());
            }
        }
    }

    private void addExports(MavenProject project, List<Export> newExports) {
        List<Export> projectExports = allExports.get(project);
        if (projectExports == null) {
            projectExports = new ArrayList<Export>();
            allExports.put(project, projectExports);
        }
        for (Export newExport : newExports) {
            projectExports.add(newExport);
        }
    }

    private void addImports(MavenProject project, List<Import> newImports) {
        List<Import> projectImports = allImports.get(project);
        if (projectImports == null) {
            projectImports = new ArrayList<Import>();
            allImports.put(project, projectImports);
        }
        for (Import newImport : newImports) {
            projectImports.add(newImport);
        }
    }

    private void injectDependenciesForMatches() {
        for (Entry<MavenProject, List<Import>> importingProjectData : allImports.entrySet()) {
            MavenProject importingProject = importingProjectData.getKey();

            for (Import importEntry : importingProjectData.getValue()) {
                injectDependenciesForMatches(importingProject, importEntry);
            }
        }
    }

    private void injectDependenciesForMatches(MavenProject importingProject, Import importEntry) {
        for (Entry<MavenProject, List<Export>> exportingProjectData : allExports.entrySet()) {
            MavenProject exportingProject = exportingProjectData.getKey();
            List<Export> exportEntries = exportingProjectData.getValue();

            if (areDifferentProjects(importingProject, exportingProject)
                    && isSatisfiedByAny(importEntry, exportEntries)) {
                injectDependencyBetween(importingProject, exportingProject);
            }
        }
    }

    private static boolean areDifferentProjects(MavenProject importingProject, MavenProject exportingProject) {
        return !exportingProject.equals(importingProject);
    }

    private static boolean isSatisfiedByAny(Import importEntry, List<Export> exports) {
        for (Export export : exports) {
            if (importEntry.isSatisfiedBy(export)) {
                return true;
            }
        }
        return false;
    }

    private void injectDependencyBetween(MavenProject from, MavenProject to) {
        from.getModel().addDependency(createProvidedScopeDependency(to));
    }

    private Dependency createProvidedScopeDependency(MavenProject dependentReactorProject) {
        Dependency dependency = new Dependency();
        dependency.setArtifactId(dependentReactorProject.getArtifactId());
        dependency.setGroupId(dependentReactorProject.getGroupId());
        dependency.setVersion(dependentReactorProject.getVersion());
        dependency.setType(dependentReactorProject.getPackaging());
        dependency.setScope(Artifact.SCOPE_PROVIDED);
        return dependency;
    }
}
