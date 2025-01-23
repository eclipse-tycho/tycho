/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;

public class SetApiBaseline implements Callable<Serializable>, Serializable {

	private boolean debug;
	private List<String> baselineBundles;
	private String name;

	SetApiBaseline(String name, Collection<Path> baselineBundles, boolean debug) {
		this.name = name;
		this.debug = debug;
		this.baselineBundles = baselineBundles.stream().map(EclipseProjectBuild::pathAsString).toList();
	}

	@Override
	public Serializable call() throws Exception {
		ILogListener listener = (status, plugin) -> debug(status.toString());
		Platform.addLogListener(listener);
		EclipseProjectBuild.disableAutoBuild();

		debug("Setting API baseline:");
		IApiBaseline baseline = ApiModelFactory.newApiBaseline(name);
		IApiComponent[] components = baselineBundles.stream().peek(this::debug).map(absoluteFile -> {
			try {
				return ApiModelFactory.newApiComponent(baseline, absoluteFile);
			} catch (CoreException e) {
				return null;
			}
		}).filter(Objects::nonNull).toArray(IApiComponent[]::new);
		baseline.addApiComponents(components);
		ApiBaselineManager.getManager().removeApiBaseline(baseline.getName());
		ApiBaselineManager.getManager().addApiBaseline(baseline);
		ApiBaselineManager.getManager().setDefaultApiBaseline(baseline.getName());
		return null;
	}

	private void debug(String string) {
		if (debug) {
			System.out.println(string);
		}
	}

}
