/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.baseline;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

public abstract class AbstractUnifiedDiff implements ResourceComparator {

	@Override
	public String compare(String resourceName, InputStream base, InputStream current) throws IOException {
		List<String> source = getLines(base);
		List<String> target = getLines(current);
			Patch<String> patch = DiffUtils.diff(source, target);
			List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(resourceName, resourceName, source, patch,
					0);
			return unifiedDiff.stream().collect(Collectors.joining((System.lineSeparator())));
	}

	protected abstract List<String> getLines(InputStream stream) throws IOException;
}
