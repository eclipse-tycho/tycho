/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.credentials;

import java.security.Principal;
import org.eclipse.equinox.security.auth.credentials.IPublicCredential;

public class EquinoxPublicCredential implements IPublicCredential {

	final private String name;
	final private Principal primaryRole;
	final private Principal[] roles;
	final private String loginModuleID;

	public EquinoxPublicCredential(String name, Principal primaryRole, String loginModuleID) {
		this.name = name;
		this.primaryRole = primaryRole;
		this.roles = null;
		this.loginModuleID = loginModuleID;
	}

	public EquinoxPublicCredential(String name, Principal[] roles, String loginModuleID) {
		this.name = name;
		this.primaryRole = null;
		this.roles = roles;
		this.loginModuleID = loginModuleID;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Principal getPrimaryRole() {
		if (primaryRole != null)
			return primaryRole;
		if (roles != null && roles.length >= 1)
			return roles[0];
		return null;
	}

	@Override
	public Principal[] getRoles() {
		return roles;
	}

	@Override
	public String getProviderID() {
		return loginModuleID;
	}

}
