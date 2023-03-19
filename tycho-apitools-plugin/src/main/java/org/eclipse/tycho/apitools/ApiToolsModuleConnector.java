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
package org.eclipse.tycho.apitools;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;

/**
 * This module connector currently do nothing else as replicate a default
 * OSGi-Framework behavior
 */
public class ApiToolsModuleConnector implements ModuleConnector {

	public ApiToolsModuleConnector() {
	}

	@Override
	public void initialize(File storage, Map<String, String> configuration) {

	}

	@Override
	public Optional<ConnectModule> connect(String location) throws BundleException {
		return Optional.empty();
	}

	@Override
	public Optional<BundleActivator> newBundleActivator() {
		return Optional.empty();
	}

}
