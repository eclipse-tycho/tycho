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
package org.eclipse.tycho.eclipsebuild;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

public class SetTargetPlatform implements Callable<Serializable>, Serializable {

	private boolean debug;
	private List<String> targetBundles;

	SetTargetPlatform(Collection<Path> dependencyBundles, boolean debug) {
		this.debug = debug;
		this.targetBundles = dependencyBundles.stream().map(EclipseBuild::pathAsString).toList();
	}

	@Override
	public Serializable call() throws Exception {
		ILogListener listener = (status, plugin) -> debug(status.toString());
		Platform.addLogListener(listener);
		EclipseBuild.disableAutoBuild();
		ITargetPlatformService service = TargetPlatformService.getDefault();
		ITargetDefinition target = service.newTarget();
		target.setName("buildpath");
		debug("== Target Platform Bundles ==");
		TargetBundle[] bundles = targetBundles.stream().peek(this::debug).map(absoluteFile -> {
			try {
				return new TargetBundle(new File(absoluteFile));
			} catch (CoreException e) {
				debug(e.toString());
				return null;
			}
		}).filter(Objects::nonNull)//
				.toArray(TargetBundle[]::new);
		target.setTargetLocations(new ITargetLocation[] { new BundleListTargetLocation(bundles) });
		service.saveTargetDefinition(target);
		Job job = new LoadTargetDefinitionJob(target);
		job.schedule();
		job.join();
		return null;
	}

	private void debug(String string) {
		if (debug) {
			System.out.println(string);
		}
	}

}
