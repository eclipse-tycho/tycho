/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.tycho.junit.platform.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

class ApplicationArguments {

	private ArrayList<String> list;

	ApplicationArguments(String[] args) {
		list = new ArrayList<>(Arrays.asList(args));
	}

	Optional<String> getArgument(String key) {
		for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			String arg = iterator.next();
			if (key.equals(arg) && iterator.hasNext()) {
				iterator.remove();
				String value = iterator.next();
				iterator.remove();
				return Optional.of(value);
			}

		}
		return Optional.empty();
	}

	boolean hasArgument(String key) {
		for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			String arg = iterator.next();
			if (key.equals(arg)) {
				iterator.remove();
				return true;
			}

		}
		return false;
	}

	List<String> getRemainingArguments(String key) {
		List<String> result = new ArrayList<>();
		boolean found = false;
		for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			String arg = iterator.next();
			if (found) {
				result.add(arg);
				iterator.remove();
			} else {
				if (key.equals(arg)) {
					iterator.remove();
					found = true;
				}
			}
		}
		return result;
	}

	String[] toArray() {
		return list.toArray(new String[0]);
	}

}
