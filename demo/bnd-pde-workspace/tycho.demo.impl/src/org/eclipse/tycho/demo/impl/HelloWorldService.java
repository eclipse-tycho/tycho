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
package org.eclipse.tycho.demo.impl;

import org.eclipse.tycho.demo.api.HelloWorld;
import org.osgi.service.component.annotations.Component;
import org.eclipse.tycho.demo.plugin.HelloWorldUtil;

@Component
public class HelloWorldService implements HelloWorld {
	public void sayHello() {
		HelloWorldUtil.printString(this, "Hello BND Workspace!");
	}
}
