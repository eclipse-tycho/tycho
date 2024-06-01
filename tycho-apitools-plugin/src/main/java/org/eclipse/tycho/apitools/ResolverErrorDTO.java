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

import java.io.Serializable;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.VersionConstraint;

public class ResolverErrorDTO implements ResolverError, Serializable {

	private final String data;
	private final int type;
	private final String toString;

	public ResolverErrorDTO(ResolverError error) {
		data = error.getData();
		type = error.getType();
		toString = error.toString();
	}

	@Override
	public BundleDescription getBundle() {
		return null;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public String getData() {
		return data;
	}

	@Override
	public VersionConstraint getUnsatisfiedConstraint() {
		return null;
	}

	@Override
	public String toString() {
		return toString;
	}

}
