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
 ******************************************************************************/
package org.eclipse.tycho.bnd.executionlistener;

import java.util.Arrays;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class PrintServices {

	@Activate
	public void activate(BundleContext bundleContext) {
		if (Boolean.getBoolean("launch.trace")) {
			System.out.println("============ Registered Services ==================");
			Arrays.stream(bundleContext.getBundles()).map(Bundle::getRegisteredServices).filter(Objects::nonNull)
					.flatMap(Arrays::stream).forEach(reference -> {
						Object service = reference.getProperty(Constants.OBJECTCLASS);
						if (service instanceof Object[]) {
							Object[] objects = (Object[]) service;
							if (objects.length == 1) {
								service = objects[0];
							} else {
								service = Arrays.toString(objects);
							}
						}
						System.out.println(service + " registered by " + reference.getBundle().getSymbolicName() + " | "
								+ reference.getProperties());
					});
		}
	}
}
