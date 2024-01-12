/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

public interface VirtualMachine {
	/**
	 * Sets request timeout in milliseconds
	 *
	 * @param timeout the timeout for the request
	 */
	public void setRequestTimeout(int timeout);

	/**
	 * @return Returns request timeout in milliseconds
	 */
	public int getRequestTimeout();
}
