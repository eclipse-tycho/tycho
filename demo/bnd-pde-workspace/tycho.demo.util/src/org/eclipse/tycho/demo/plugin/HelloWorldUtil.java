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
package org.eclipse.tycho.demo.plugin;

import org.eclipse.tycho.demo.api.HelloWorld;

public interface HelloWorldUtil {
	
	public static void printString(HelloWorld caller, String msg) {
		System.out.println("["+caller.getClass().getSimpleName()+"] "+msg);
	}
}
