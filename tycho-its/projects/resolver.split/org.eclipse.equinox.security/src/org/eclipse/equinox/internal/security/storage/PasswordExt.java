/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.storage;

import javax.crypto.spec.PBEKeySpec;

public class PasswordExt {

	final public PBEKeySpec password;

	final public String moduleID;

	public PasswordExt(PBEKeySpec password, String moduleID) {
		super();
		this.moduleID = moduleID;
		this.password = password;
	}

	public PBEKeySpec getPassword() {
		return password;
	}

	public String getModuleID() {
		return moduleID;
	}
}
