/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.storage.provider;

public interface IValidatingPasswordProvider {

	/**
	 * Return if password provider is valid for current system
	 * @return true if valid, false otherwise
	 */
	public boolean isValid();

}
