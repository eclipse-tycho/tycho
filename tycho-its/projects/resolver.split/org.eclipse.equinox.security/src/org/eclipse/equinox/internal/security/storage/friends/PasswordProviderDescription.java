/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.storage.friends;

import java.util.List;

/**
 * This class is used to pass description of a password provider module.
 */
public class PasswordProviderDescription {

	static final private String EMPTY_STRING = ""; //$NON-NLS-1$

	private int priority;
	private String id;
	private String name;
	private String description;
	private List<String> hints;

	public PasswordProviderDescription(String name, String id, int priority, String description, List<String> hints) {
		this.id = id;
		this.name = name;
		this.priority = priority;
		this.description = description;
		this.hints = hints;
	}

	public int getPriority() {
		return priority;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return (description == null) ? EMPTY_STRING : description;
	}

	public boolean hasHint(String hint) {
		if (hints == null)
			return false;
		for (String candidate : hints) {
			if (hint.equalsIgnoreCase(candidate))
				return true;
		}
		return false;
	}

	public String getName() {
		if (name == null || name.length() == 0)
			return id;
		return name;
	}
}
