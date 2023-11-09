/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdi;

public class Bootstrap {
	private static com.sun.jdi.VirtualMachineManager fVirtualMachineManager;

	public Bootstrap() {
	}

	public static synchronized com.sun.jdi.VirtualMachineManager virtualMachineManager() {

		return fVirtualMachineManager;
	}
}
