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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.bnd.maven;

import java.util.Iterator;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.MojoExecution;

@Named
@Singleton
public class BndProjectExecutionListener implements ProjectExecutionListener {

	@Override
	public void beforeProjectExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {

	}

	@Override
	public void beforeProjectLifecycleExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {
		if (BndMavenLifecycleParticipant.isBNDProject(event.getProject())) {
			List<MojoExecution> executionPlan = event.getExecutionPlan();
			for (Iterator<MojoExecution> iterator = executionPlan.iterator(); iterator.hasNext();) {
				MojoExecution mojoExecution = iterator.next();
				if (isUnwantedExecution(mojoExecution)) {
					iterator.remove();
				}
			}
		}
	}

	private boolean isUnwantedExecution(MojoExecution mojoExecution) {
		if (matches(mojoExecution, "org.apache.maven.plugins", "maven-compiler-plugin", "default-compile")) {
			return true;
		}
		if (matches(mojoExecution, "org.apache.maven.plugins", "maven-compiler-plugin", "default-testCompile")) {
			return true;
		}
		if (matches(mojoExecution, "org.apache.maven.plugins", "maven-surefire-plugin", "default-test")) {
			return true;
		}
		if (matches(mojoExecution, "org.apache.maven.plugins", "maven-jar-plugin", "default-jar")) {
			return true;
		}
		if (matches(mojoExecution, "org.apache.maven.plugins", "maven-clean-plugin", "default-clean")) {
			return true;
		}
		if (matches(mojoExecution, "org.apache.maven.plugins", "maven-resources-plugin", "default-resources")) {
			return true;
		}
		if (matches(mojoExecution, "org.apache.maven.plugins", "maven-resources-plugin", "default-testResources")) {
			return true;
		}
		return false;
	}

	private boolean matches(MojoExecution mojoExecution, String groupId, String artifactId, String id) {
		return id.equals(mojoExecution.getExecutionId()) && groupId.equals(mojoExecution.getGroupId())
				&& artifactId.equals(mojoExecution.getArtifactId());
	}

	@Override
	public void afterProjectExecutionSuccess(ProjectExecutionEvent event) throws LifecycleExecutionException {

	}

	@Override
	public void afterProjectExecutionFailure(ProjectExecutionEvent event) {

	}

}
